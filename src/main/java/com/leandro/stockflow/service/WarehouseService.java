package com.leandro.stockflow.service;

import com.leandro.stockflow.dto.WarehouseRequest;
import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.entity.Warehouse;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.mapper.WarehouseMapper;
import com.leandro.stockflow.repository.WarehouseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WarehouseService {

  private final WarehouseRepository warehouseRepository;

  public WarehouseService(WarehouseRepository warehouseRepository) {
    this.warehouseRepository = warehouseRepository;
  }

  @Transactional
  public WarehouseResponse create(WarehouseRequest request) {
    Warehouse warehouse = new Warehouse(request.name(), request.location());
    return WarehouseMapper.toResponse(warehouseRepository.save(warehouse));
  }

  @Transactional(readOnly = true)
  public List<WarehouseResponse> findAll() {
    return warehouseRepository.findAll().stream().map(WarehouseMapper::toResponse).toList();
  }

  protected Warehouse getOrThrow(Long id) {
    return warehouseRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
  }
}
