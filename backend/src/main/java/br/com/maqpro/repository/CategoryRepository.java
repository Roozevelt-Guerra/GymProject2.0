package br.com.maqpro.repository;

import br.com.maqpro.entity.Category;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<Category, Long> {
  Optional<Category> findByName(String name);

  // A unique constraint and PostgreSQL's upsert also handle simultaneous requests/instances.
  @Modifying
  @Query(value = "INSERT INTO category(name) VALUES (:name) ON CONFLICT (name) DO NOTHING",
      nativeQuery = true)
  void createIfAbsent(@Param("name") String name);
}
