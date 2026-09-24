package br.com.maqpro.repository;

import br.com.maqpro.entity.Equipment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
  @EntityGraph(attributePaths = "category")
  List<Equipment> findAllByOrderByIdAsc();
}
