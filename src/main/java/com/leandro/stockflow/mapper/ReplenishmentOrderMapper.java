package com.leandro.stockflow.mapper;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
import com.leandro.stockflow.entity.ReplenishmentOrder;

public class ReplenishmentOrderMapper {

  private ReplenishmentOrderMapper() {}

  public static ReplenishmentOrderResponse toResponse(ReplenishmentOrder order) {
    return new ReplenishmentOrderResponse(
        order.getId(),
        order.getProduct().getSku(),
        order.getProduct().getName(),
        order.getWarehouse().getName(),
        order.getRequestedQuantity(),
        order.getStatus(),
        order.getCreatedAt(),
        order.getCompletedAt());
  }
}
