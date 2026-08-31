package com.leandro.stockflow.dto;

import com.leandro.stockflow.entity.MovementType;
import java.time.LocalDateTime;

public record StockMovementResponse(
    Long id,
    String productSku,
    String productName,
    String warehouseName,
    MovementType type,
    int quantity,
    String reason,
    String reference,
    LocalDateTime occurredAt) {}
