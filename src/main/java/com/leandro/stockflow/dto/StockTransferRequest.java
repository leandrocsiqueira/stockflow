package com.leandro.stockflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StockTransferRequest(
    @NotNull(message = "Product id is required") Long productId,
    @NotNull(message = "Source warehouse id is required") Long sourceWarehouseId,
    @NotNull(message = "Destination warehouse id is required") Long destinationWarehouseId,
    @Min(value = 1, message = "Quantity must be positive") int quantity,
    @Size(max = 255, message = "Reason must not exceed 255 characters") String reason,
    @NotBlank(message = "Reference is required")
        @Size(max = 100, message = "Reference must not exceed 100 characters")
        String reference) {}
