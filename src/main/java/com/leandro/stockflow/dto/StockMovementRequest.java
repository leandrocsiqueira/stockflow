package com.leandro.stockflow.dto;

import com.leandro.stockflow.entity.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockMovementRequest(
    @NotNull(message = "Product id is required") Long productId,
    @NotNull(message = "Warehouse id is required") Long warehouseId,
    @NotNull(message = "Movement type is required") MovementType type,
    @Min(value = 1, message = "Quantity must be positive") int quantity,
    String reason,
    String reference) {}
