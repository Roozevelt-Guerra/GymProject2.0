package br.com.maqpro.service;

import br.com.maqpro.dto.EquipmentDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogImporter {
  public record Catalog(Long nextId, List<EquipmentDto> equipment) {}
  private static final Logger log = LoggerFactory.getLogger(CatalogImporter.class);
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final Validator validator;
  private final Path file;

  public CatalogImporter(JdbcTemplate jdbc, ObjectMapper mapper, Validator validator,
      @Value("${app.catalog-file}") String filename) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.validator = validator;
    this.file = Path.of(filename).toAbsolutePath().normalize();
  }

  @Transactional
  public void importOnce() {
    // Lock first: a second instance waits and then observes the committed completion marker.
    Boolean completed = jdbc.queryForObject(
        "SELECT completed_at IS NOT NULL FROM catalog_import WHERE id = 1 FOR UPDATE",
        Boolean.class);
    if (Boolean.TRUE.equals(completed)) return;
    if (!Files.isRegularFile(file)) {
      throw new IllegalStateException("Arquivo de importação ausente ou inacessível: " + file
          + ". Disponibilize o JSON ou defina CATALOG_IMPORT_ENABLED=false para um banco novo.");
    }
    Catalog catalog;
    try {
      catalog = mapper.readerFor(Catalog.class)
          .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(file.toFile());
      validate(catalog);
    } catch (Exception e) {
      // Avoid logging record contents (or replacing the source file) on validation failures.
      throw new IllegalStateException("Catálogo inválido para importação: " + file
          + ". Corrija o arquivo; nenhum equipamento foi importado.");
    }
    if (jdbc.queryForObject("SELECT count(*) FROM equipment", Long.class) != 0
        || jdbc.queryForObject("SELECT count(*) FROM category", Long.class) != 0) {
      throw new IllegalStateException("Importação recusada: o banco já possui dados sem registro de importação."
          + " Use um banco vazio ou revise os dados e desative CATALOG_IMPORT_ENABLED.");
    }
    for (EquipmentDto item : catalog.equipment()) {
      jdbc.update("INSERT INTO category(name) VALUES (?) ON CONFLICT (name) DO NOTHING", item.category().trim());
      jdbc.update("""
          INSERT INTO equipment(id, name, description, price, image_url, category_id, active)
          VALUES (?, ?, ?, ?, ?, (SELECT id FROM category WHERE name = ?), ?)
          """, item.id(), item.name(), item.description(), item.price(), item.imageUrl(),
          item.category().trim(), item.active() == null || item.active());
    }
    // Retain nextId as well as existing IDs, including the IDs of items deleted from the JSON.
    // PostgreSQL sequence gaps after a rolled-back import are harmless; retries reset it safely.
    jdbc.queryForObject("SELECT setval(pg_get_serial_sequence('equipment', 'id'), ?, false)",
        Long.class, catalog.nextId());
    jdbc.update("UPDATE catalog_import SET completed_at = CURRENT_TIMESTAMP, imported_count = ? WHERE id = 1",
        catalog.equipment().size());
    log.info("Importação do catálogo concluída: {} equipamentos", catalog.equipment().size());
  }

  private void validate(Catalog catalog) {
    if (catalog == null || catalog.nextId() == null || catalog.nextId() < 1
        || catalog.equipment() == null) throw new IllegalArgumentException();
    var ids = new HashSet<Long>();
    for (EquipmentDto item : catalog.equipment()) {
      if (item == null || item.id() == null || item.id() < 1 || item.id() >= catalog.nextId()
          || !ids.add(item.id()) || !validator.validate(item).isEmpty()) {
        throw new IllegalArgumentException();
      }
    }
  }
}
