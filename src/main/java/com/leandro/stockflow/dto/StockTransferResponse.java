package com.leandro.stockflow.dto;

import java.time.LocalDateTime;

public record StockTransferResponse(
    Long sourceMovementId,
    Long destinationMovementId,
    String productSku,
    String productName,
    String sourceWarehouseName,
    String destinationWarehouseName,
    int quantity,
    String reason,
    String reference,
    int sourceBalance,
    int destinationBalance,
    LocalDateTime occurredAt) {}
