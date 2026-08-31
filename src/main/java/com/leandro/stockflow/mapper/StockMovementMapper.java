package com.leandro.stockflow.mapper;

import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.entity.StockMovement;

public class StockMovementMapper {

  private StockMovementMapper() {}

  public static StockMovementResponse toResponse(StockMovement movement) {
    return new StockMovementResponse(
        movement.getId(),
        movement.getProduct().getSku(),
        movement.getProduct().getName(),
        movement.getWarehouse().getName(),
        movement.getType(),
        movement.getQuantity(),
        movement.getReason(),
        movement.getReference(),
        movement.getOccurredAt());
  }
}
