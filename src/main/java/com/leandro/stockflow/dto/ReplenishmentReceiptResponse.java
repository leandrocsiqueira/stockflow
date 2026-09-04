package com.leandro.stockflow.dto;

import java.time.LocalDateTime;

public record ReplenishmentReceiptResponse(
    Long orderId,
    Long movementId,
    String productSku,
    String productName,
    String warehouseName,
    int receivedQuantity,
    int resultingBalance,
    String reference,
    LocalDateTime receivedAt) {}
