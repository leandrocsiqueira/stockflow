package com.leandro.stockflow.dto;

public record StockResponse(
    Long id,
    String productSku,
    String productName,
    String warehouseName,
    int quantity,
    int minimumStock,
    boolean belowMinimum) {}
