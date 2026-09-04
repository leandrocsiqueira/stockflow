package com.leandro.stockflow.dto;

import com.leandro.stockflow.entity.ReplenishmentStatus;
import java.time.LocalDateTime;

public record ReplenishmentOrderResponse(
    Long id,
    String productSku,
    String productName,
    String warehouseName,
    int requestedQuantity,
    ReplenishmentStatus status,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    LocalDateTime cancelledAt) {}
