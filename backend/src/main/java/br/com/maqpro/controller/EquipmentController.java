package br.com.maqpro.controller;

import br.com.maqpro.dto.EquipmentDto;
import br.com.maqpro.service.EquipmentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
  private final EquipmentService service;

  public EquipmentController(EquipmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<EquipmentDto> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public EquipmentDto get(@PathVariable long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EquipmentDto create(@Valid @RequestBody EquipmentDto data) {
    return service.save(null, data);
  }

  @PutMapping("/{id}")
  public EquipmentDto update(@PathVariable long id, @Valid @RequestBody EquipmentDto data) {
    return service.save(id, data);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable long id) {
    service.delete(id);
  }
}
