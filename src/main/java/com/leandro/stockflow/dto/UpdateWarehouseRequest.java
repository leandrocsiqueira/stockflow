package com.leandro.stockflow.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWarehouseRequest(
    @NotBlank(message = "Name is required") String name, String location) {}
