package br.com.maqpro.repository;

import br.com.maqpro.entity.Equipment;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/** Single-process storage. Each change replaces the complete file atomically. */
@Repository
public class EquipmentRepository {
  public record Catalog(long nextId, List<Equipment> equipment) {}

  private final ObjectMapper mapper;
  private final Path file;
  private Catalog catalog;

  public EquipmentRepository(ObjectMapper mapper, @Value("${app.catalog-file}") String filename) {
    this.mapper = mapper;
    this.file = Path.of(filename).toAbsolutePath().normalize();
    try {
      if (Files.notExists(file)) {
        catalog = new Catalog(1, List.of());
      } else {
        catalog =
            mapper
                .readerFor(Catalog.class)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .readValue(file.toFile());
        validate(catalog);
        catalog = new Catalog(catalog.nextId(), List.copyOf(catalog.equipment()));
      }
    } catch (IOException | IllegalArgumentException e) {
      throw new IllegalStateException(
          "Não foi possível ler o catálogo: " + file + ". O arquivo foi preservado.", e);
    }
  }

  private void validate(Catalog value) {
    if (value == null || value.nextId() < 1 || value.equipment() == null)
      throw new IllegalArgumentException("Catálogo inválido");
    Set<Long> ids = new HashSet<>();
    for (Equipment e : value.equipment()) {
      if (e == null
          || e.id() == null
          || e.id() < 1
          || e.id() >= value.nextId()
          || !ids.add(e.id())
          || e.name() == null
          || e.name().isBlank()
          || e.description() == null
          || e.description().isBlank()
          || e.price() == null
          || e.price().signum() <= 0
          || e.imageUrl() == null
          || e.category() == null)
        throw new IllegalArgumentException("Equipamento inválido no catálogo");
    }
  }

  public synchronized List<Equipment> findAll() {
    return catalog.equipment();
  }

  public synchronized Optional<Equipment> findById(long id) {
    return catalog.equipment().stream().filter(e -> e.id() == id).findFirst();
  }

  public synchronized Equipment save(Equipment equipment) {
    Long id = equipment.id();
    if (id != null && findById(id).isEmpty())
      throw new NoSuchElementException("Equipamento não encontrado");
    long nextId = catalog.nextId();
    if (id == null) {
      id = nextId;
      nextId = Math.incrementExact(nextId);
    }
    Equipment saved =
        new Equipment(
            id,
            equipment.name(),
            equipment.description(),
            equipment.price(),
            equipment.imageUrl(),
            equipment.category());
    List<Equipment> updated = new ArrayList<>(catalog.equipment());
    updated.removeIf(e -> e.id().equals(saved.id()));
    updated.add(saved);
    updated.sort(Comparator.comparing(Equipment::id));
    persist(new Catalog(nextId, List.copyOf(updated)));
    return saved;
  }

  public synchronized void delete(Equipment equipment) {
    List<Equipment> updated =
        catalog.equipment().stream().filter(e -> !e.id().equals(equipment.id())).toList();
    persist(new Catalog(catalog.nextId(), updated));
  }

  private void persist(Catalog updated) {
    Path temporary = null;
    try {
      Files.createDirectories(file.getParent());
      temporary = Files.createTempFile(file.getParent(), ".equipment-", ".tmp");
      mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), updated);
      Files.move(
          temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      catalog = updated;
    } catch (IOException e) {
      throw new UncheckedIOException("Não foi possível salvar o catálogo: " + file, e);
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
          /* Original failure is preserved. */
        }
      }
    }
  }
}
