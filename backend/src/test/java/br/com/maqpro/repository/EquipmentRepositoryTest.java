package br.com.maqpro.repository;

import static org.junit.jupiter.api.Assertions.*;

import br.com.maqpro.entity.Equipment;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EquipmentRepositoryTest {
  @TempDir Path directory;

  EquipmentRepository open(Path file) {
    return new EquipmentRepository(new ObjectMapper(), file.toString());
  }

  Equipment item(Long id, String name) {
    return new Equipment(
        id,
        name,
        "Descrição",
        new BigDecimal("1250.50"),
        "https://example.com/photo.jpg",
        "Musculação");
  }

  @Test
  void savesUpdatesAndDeletesSurviveReopeningWithoutReusingIds() {
    Path file = directory.resolve("data/equipment.json");
    var first = open(file);
    assertTrue(first.findAll().isEmpty());
    Equipment saved = first.save(item(null, "Leg press"));
    var second = open(file);
    assertEquals(saved, second.findById(saved.id()).orElseThrow());
    second.save(item(saved.id(), "Leg press atualizado"));
    var third = open(file);
    assertEquals("Leg press atualizado", third.findById(saved.id()).orElseThrow().name());
    third.delete(saved);
    var fourth = open(file);
    assertTrue(fourth.findAll().isEmpty());
    assertTrue(fourth.save(item(null, "Novo equipamento")).id() > saved.id());
  }

  @Test
  void corruptFileIsNotReplaced() throws Exception {
    Path file = directory.resolve("equipment.json");
    Files.writeString(file, "{broken json");
    assertThrows(IllegalStateException.class, () -> open(file));
    assertEquals("{broken json", Files.readString(file));
    Files.writeString(file, "{\"nextId\":1,\"equipment\":null}");
    assertThrows(IllegalStateException.class, () -> open(file));
  }

  @Test
  void failedWriteDoesNotChangeMemoryOrExistingFile() throws Exception {
    Path folder = directory.resolve("data");
    Files.createDirectory(folder);
    var repository = open(folder.resolve("equipment.json"));
    Equipment saved = repository.save(item(null, "Original"));
    Path backup = directory.resolve("backup");
    Files.move(folder, backup);
    Files.writeString(folder, "block parent directory");
    assertThrows(UncheckedIOException.class, () -> repository.save(item(saved.id(), "Alterado")));
    assertEquals(saved, repository.findById(saved.id()).orElseThrow());
    assertEquals(saved, open(backup.resolve("equipment.json")).findById(saved.id()).orElseThrow());
  }

  @Test
  void concurrentCreatesKeepEveryItemWithUniqueIds() throws Exception {
    Path file = directory.resolve("equipment.json");
    var repository = open(file);
    try (var executor = Executors.newFixedThreadPool(4)) {
      var futures =
          IntStream.range(0, 20)
              .mapToObj(n -> executor.submit(() -> repository.save(item(null, "Item " + n))))
              .toList();
      for (var future : futures) future.get();
    }
    var reloaded = open(file).findAll();
    assertEquals(20, reloaded.size());
    assertEquals(20L, reloaded.stream().map(Equipment::id).distinct().count());
  }
}
