package com.leandro.stockflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateProductRequest(
    @NotBlank(message = "SKU is required") String sku,
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Unit is required") String unit,
    @Min(value = 0, message = "Minimum stock cannot be negative") int minimumStock) {}
