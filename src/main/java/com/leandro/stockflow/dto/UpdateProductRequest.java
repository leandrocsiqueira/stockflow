package com.leandro.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProductRequest(@NotBlank(message = "Name is required") String name) {}
