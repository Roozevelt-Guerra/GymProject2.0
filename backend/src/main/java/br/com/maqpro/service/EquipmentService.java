package br.com.maqpro.service;

import br.com.maqpro.dto.EquipmentDto;
import br.com.maqpro.entity.Equipment;
import br.com.maqpro.repository.EquipmentRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EquipmentService {
  private final EquipmentRepository repository;

  public EquipmentService(EquipmentRepository repository) {
    this.repository = repository;
  }

  private Equipment find(long id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipamento não encontrado"));
  }

  public EquipmentDto get(long id) {
    return dto(find(id));
  }

  public List<EquipmentDto> list() {
    return repository.findAll().stream().map(this::dto).toList();
  }

  public synchronized EquipmentDto save(Long id, EquipmentDto data) {
    if (id != null) find(id);
    Equipment e =
        new Equipment(
            id,
            data.name().trim(),
            data.description().trim(),
            data.price(),
            data.imageUrl(),
            data.category().trim());
    return dto(repository.save(e));
  }

  public synchronized void delete(long id) {
    repository.delete(find(id));
  }

  private EquipmentDto dto(Equipment e) {
    return new EquipmentDto(
        e.id(), e.name(), e.description(), e.price(), e.imageUrl(), e.category());
  }
}
