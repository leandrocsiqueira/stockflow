package com.leandro.stockflow.dto;

public record ProductResponse(Long id, String sku, String name, String unit, int minimumStock) {}
