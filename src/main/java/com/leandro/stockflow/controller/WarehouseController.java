package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.WarehouseRequest;
import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.service.WarehouseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
@Tag(name = "Warehouses")
public class WarehouseController {

  private final WarehouseService warehouseService;

  public WarehouseController(WarehouseService warehouseService) {
    this.warehouseService = warehouseService;
  }

  @PostMapping
  public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
  }

  @GetMapping
  public ResponseEntity<List<WarehouseResponse>> findAll() {
    return ResponseEntity.ok(warehouseService.findAll());
  }
}
