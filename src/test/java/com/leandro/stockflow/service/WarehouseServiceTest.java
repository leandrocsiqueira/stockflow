package com.leandro.stockflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leandro.stockflow.dto.CreateWarehouseRequest;
import com.leandro.stockflow.dto.UpdateWarehouseRequest;
import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.entity.Warehouse;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.repository.WarehouseRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

  @Mock private WarehouseRepository warehouseRepository;

  @InjectMocks private WarehouseService warehouseService;

  @Test
  void shouldCreateWarehouse() {
    CreateWarehouseRequest request = new CreateWarehouseRequest("Main Warehouse", "SP");

    when(warehouseRepository.save(any(Warehouse.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    WarehouseResponse response = warehouseService.create(request);

    assertThat(response.name()).isEqualTo("Main Warehouse");
    assertThat(response.location()).isEqualTo("SP");
    verify(warehouseRepository).save(any(Warehouse.class));
  }

  @Test
  void shouldFindWarehouseById() {
    Warehouse warehouse = new Warehouse("Main Warehouse", "SP");
    when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

    WarehouseResponse response = warehouseService.findById(1L);

    assertThat(response.name()).isEqualTo("Main Warehouse");
    assertThat(response.location()).isEqualTo("SP");
  }

  @Test
  void shouldUpdateWarehouse() {
    Warehouse warehouse = new Warehouse("Old Warehouse", "RJ");
    UpdateWarehouseRequest request = new UpdateWarehouseRequest("Main Warehouse", "SP");

    when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

    WarehouseResponse response = warehouseService.update(1L, request);

    assertThat(response.name()).isEqualTo("Main Warehouse");
    assertThat(response.location()).isEqualTo("SP");
  }

  @Test
  void shouldThrowWhenUpdatingMissingWarehouse() {
    when(warehouseRepository.findById(999L)).thenReturn(Optional.empty());
    UpdateWarehouseRequest request = new UpdateWarehouseRequest("Main Warehouse", "SP");

    assertThatThrownBy(() -> warehouseService.update(999L, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Warehouse not found with id: 999");
  }
}
