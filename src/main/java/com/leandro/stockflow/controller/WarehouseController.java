package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.CreateWarehouseRequest;
import com.leandro.stockflow.dto.UpdateWarehouseRequest;
import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.exception.ApiError;
import com.leandro.stockflow.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
@Tag(
    name = "Warehouses",
    description = "Warehouse registration, listing, retrieval, and update operations")
public class WarehouseController {
  private final WarehouseService warehouseService;

  public WarehouseController(WarehouseService warehouseService) {
    this.warehouseService = warehouseService;
  }

  @PostMapping
  @Operation(summary = "Create a warehouse")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Warehouse created successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<WarehouseResponse> create(
      @Valid @RequestBody CreateWarehouseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
  }

  @GetMapping
  @Operation(summary = "List warehouses")
  @ApiResponse(responseCode = "200", description = "Warehouses retrieved successfully")
  public ResponseEntity<List<WarehouseResponse>> findAll() {
    return ResponseEntity.ok(warehouseService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get warehouse by ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Warehouse retrieved successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Warehouse not found",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<WarehouseResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(warehouseService.findById(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a warehouse")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Warehouse updated successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Warehouse not found",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<WarehouseResponse> update(
      @PathVariable Long id, @Valid @RequestBody UpdateWarehouseRequest request) {
    return ResponseEntity.ok(warehouseService.update(id, request));
  }
}
