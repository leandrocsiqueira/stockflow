package com.leandro.stockflow.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockPolicyRequest(
    @NotNull(message = "Product id is required") Long productId,
    @NotNull(message = "Warehouse id is required") Long warehouseId,
    @Min(value = 0, message = "Reorder point cannot be negative") int reorderPoint,
    @Min(value = 0, message = "Target stock cannot be negative") int targetStock) {

  @AssertTrue(message = "Target stock must be greater than or equal to reorder point")
  public boolean isTargetStockValid() {
    return targetStock >= reorderPoint;
  }
}
