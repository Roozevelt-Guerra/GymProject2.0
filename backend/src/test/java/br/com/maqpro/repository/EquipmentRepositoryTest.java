package br.com.maqpro.repository;

import static org.junit.jupiter.api.Assertions.*;

import br.com.maqpro.PostgresTestSupport;
import br.com.maqpro.dto.EquipmentDto;
import br.com.maqpro.service.EquipmentService;
import java.math.BigDecimal;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = "app.admin-password=test-password-123")
class EquipmentRepositoryTest extends PostgresTestSupport {
  @Autowired EquipmentService service;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void clear() { jdbc.execute("TRUNCATE equipment, category RESTART IDENTITY CASCADE"); }

  EquipmentDto item(String name, String category, Boolean active) {
    return new EquipmentDto(null, name, "Descrição", new BigDecimal("1250.50"),
        "https://example.com/photo.jpg", category, active);
  }

  @Test
  void committedCrudAndCategoryRelationship() {
    var saved = service.save(null, item("Leg press", "Musculação", null));
    assertTrue(saved.active());
    assertEquals(saved, service.get(saved.id()));
    assertEquals("Musculação", jdbc.queryForObject(
        "SELECT c.name FROM equipment e JOIN category c ON c.id = e.category_id WHERE e.id = ?",
        String.class, saved.id()));
    service.save(saved.id(), item("Atualizado", "Cardio", false));
    assertFalse(service.get(saved.id()).active());
    service.save(saved.id(), item("Angular omite active", "Cardio", null));
    assertFalse(service.get(saved.id()).active());
    assertEquals("Cardio", service.get(saved.id()).category());
    service.delete(saved.id());
    assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM equipment", Long.class));
    assertTrue(service.save(null, item("Novo", "Cardio", null)).id() > saved.id());
  }

  @Test
  void concurrentCreatesShareOneCategoryAndHaveUniqueIds() throws Exception {
    try (var executor = Executors.newFixedThreadPool(4)) {
      var futures = IntStream.range(0, 20)
          .mapToObj(n -> executor.submit(() -> service.save(null, item("Item " + n, "Musculação", null))))
          .toList();
      for (var future : futures) future.get();
    }
    assertEquals(20, service.list().size());
    assertEquals(20, service.list().stream().map(EquipmentDto::id).distinct().count());
    assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM category", Long.class));
  }
}
