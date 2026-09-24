package br.com.maqpro.service;

import static org.junit.jupiter.api.Assertions.*;

import br.com.maqpro.PostgresTestSupport;
import br.com.maqpro.dto.EquipmentDto;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(properties = "app.admin-password=test-password-123")
class CatalogImporterTest extends PostgresTestSupport {
  private static final Path FILE = temporaryFile();
  private static Path temporaryFile() {
    try { return Files.createTempDirectory("catalog-import-test-").resolve("equipment.json"); }
    catch (Exception e) { throw new ExceptionInInitializerError(e); }
  }
  @DynamicPropertySource
  static void file(DynamicPropertyRegistry registry) {
    registry.add("app.catalog-file", FILE::toString);
  }
  @Autowired CatalogImporter importer;
  @Autowired EquipmentService service;
  @Autowired JdbcTemplate jdbc;
  private static final String JSON = """
      {"nextId":50,"equipment":[
      {"id":2,"name":"Leg press","description":"Estrutura profissional","price":12000.50,
       "imageUrl":"https://example.com/leg.jpg","category":"Musculação"},
      {"id":7,"name":"Supino","description":"Banco","price":1000.00,
       "imageUrl":"https://example.com/banco.jpg","category":"Musculação"}]}
      """;

  @BeforeEach
  void reset() throws Exception {
    jdbc.execute("TRUNCATE equipment, category RESTART IDENTITY CASCADE");
    jdbc.update("UPDATE catalog_import SET completed_at = NULL, imported_count = NULL WHERE id = 1");
    Files.writeString(FILE, JSON);
  }

  @Test
  void importsOncePreservingIdsPricesNextIdAndDeletions() throws Exception {
    importer.importOnce();
    assertEquals(2, service.list().size());
    assertEquals(new BigDecimal("12000.50"), service.get(2).price());
    assertTrue(service.get(7).active());
    assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM category", Long.class));
    service.delete(2);
    importer.importOnce();
    assertEquals(1, service.list().size());
    assertEquals(JSON, Files.readString(FILE));
    Files.delete(FILE); // A completed import no longer requires the old file.
    importer.importOnce();
    var created = service.save(null, new EquipmentDto(null, "Novo", "Descrição",
        new BigDecimal("10.00"), "https://example.com/item.jpg", "Cardio", null));
    assertEquals(50L, created.id());
    assertEquals(2, jdbc.queryForObject("SELECT imported_count FROM catalog_import", Integer.class));
  }

  @Test
  void invalidMissingDuplicateAndTrailingDataNeverImportPartially() throws Exception {
    for (String invalid : new String[] {"{broken", "{\"nextId\":1,\"equipment\":null}",
        JSON.replace("\"id\":7", "\"id\":2"), JSON.replace("1000.00", "-1"), JSON + " {}"}) {
      Files.writeString(FILE, invalid);
      assertThrows(IllegalStateException.class, importer::importOnce);
      assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM equipment", Long.class));
      assertEquals(invalid, Files.readString(FILE));
    }
    Files.delete(FILE);
    assertThrows(IllegalStateException.class, importer::importOnce);
    Files.writeString(FILE, JSON);
    importer.importOnce(); // Can retry after correcting the source.
    assertEquals(2, service.list().size());
  }

  @Test
  void occupiedDatabaseIsNotOverwritten() {
    jdbc.update("INSERT INTO category(name) VALUES ('Existente')");
    assertThrows(IllegalStateException.class, importer::importOnce);
    assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM equipment", Long.class));
    assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM category", Long.class));
  }

  @Test
  void databaseFailureRollsBackAllRowsAndAllowsRetry() {
    jdbc.execute("ALTER TABLE equipment ADD CONSTRAINT reject_test_item CHECK (name <> 'Supino')");
    try {
      assertThrows(RuntimeException.class, importer::importOnce);
      assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM equipment", Long.class));
      assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM category", Long.class));
      assertFalse(jdbc.queryForObject("SELECT completed_at IS NOT NULL FROM catalog_import", Boolean.class));
    } finally {
      jdbc.execute("ALTER TABLE equipment DROP CONSTRAINT reject_test_item");
    }
    importer.importOnce();
    assertEquals(2, service.list().size());
  }

  @Test
  void simultaneousImportsDoNotDuplicate() throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first = executor.submit(() -> importer.importOnce());
      var second = executor.submit(() -> importer.importOnce());
      first.get();
      second.get();
    }
    assertEquals(2, service.list().size());
  }
}
