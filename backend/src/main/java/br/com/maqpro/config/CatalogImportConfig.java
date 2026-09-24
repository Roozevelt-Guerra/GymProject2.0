package br.com.maqpro.config;

import br.com.maqpro.service.CatalogImporter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogImportConfig {
  @Bean
  @ConditionalOnProperty(name = "app.catalog-import-enabled", havingValue = "true", matchIfMissing = true)
  ApplicationRunner importCatalog(CatalogImporter importer) {
    return args -> importer.importOnce();
  }
}
