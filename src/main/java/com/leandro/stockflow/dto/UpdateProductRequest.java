package com.leandro.stockflow.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UpdateProductRequest(
    @NotBlank(message = "Name is required") String name,
    @Min(value = 0, message = "Minimum stock cannot be negative") int minimumStock) {}
