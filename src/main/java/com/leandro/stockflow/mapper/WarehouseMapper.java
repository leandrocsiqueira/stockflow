package com.leandro.stockflow.mapper;

import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.entity.Warehouse;

public class WarehouseMapper {

  private WarehouseMapper() {}

  public static WarehouseResponse toResponse(Warehouse warehouse) {
    return new WarehouseResponse(warehouse.getId(), warehouse.getName(), warehouse.getLocation());
  }
}
