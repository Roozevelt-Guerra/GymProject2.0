package br.com.maqpro.service;

import br.com.maqpro.dto.EquipmentDto;
import br.com.maqpro.entity.Equipment;
import br.com.maqpro.repository.CategoryRepository;
import br.com.maqpro.repository.EquipmentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class EquipmentService {
  private final EquipmentRepository repository;
  private final CategoryRepository categories;

  public EquipmentService(EquipmentRepository repository, CategoryRepository categories) {
    this.repository = repository;
    this.categories = categories;
  }

  private Equipment find(long id) {
    return repository.findById(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
  }

  public EquipmentDto get(long id) { return dto(find(id)); }

  public List<EquipmentDto> list() {
    return repository.findAllByOrderByIdAsc().stream().map(this::dto).toList();
  }

  @Transactional
  public EquipmentDto save(Long id, EquipmentDto data) {
    Equipment equipment = id == null ? new Equipment() : find(id);
    String category = data.category().trim();
    categories.createIfAbsent(category);
    equipment.update(data.name().trim(), data.description().trim(), data.price(),
        data.imageUrl(), categories.findByName(category).orElseThrow(), data.active());
    return dto(repository.save(equipment));
  }

  @Transactional
  public void delete(long id) { repository.delete(find(id)); }

  private EquipmentDto dto(Equipment e) {
    return new EquipmentDto(e.getId(), e.getName(), e.getDescription(), e.getPrice(),
        e.getImageUrl(), e.getCategory().getName(), e.isActive());
  }
}
