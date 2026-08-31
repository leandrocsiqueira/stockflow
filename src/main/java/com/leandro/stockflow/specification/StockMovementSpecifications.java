package com.leandro.stockflow.specification;

import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.entity.StockMovement;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public class StockMovementSpecifications {

  private StockMovementSpecifications() {}

  public static Specification<StockMovement> productIdEquals(Long productId) {
    return (root, query, cb) ->
        productId == null ? null : cb.equal(root.get("product").get("id"), productId);
  }

  public static Specification<StockMovement> warehouseIdEquals(Long warehouseId) {
    return (root, query, cb) ->
        warehouseId == null ? null : cb.equal(root.get("warehouse").get("id"), warehouseId);
  }

  public static Specification<StockMovement> typeEquals(MovementType type) {
    return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
  }

  public static Specification<StockMovement> occurredAfter(LocalDateTime from) {
    return (root, query, cb) ->
        from == null ? null : cb.greaterThanOrEqualTo(root.get("occurredAt"), from);
  }

  public static Specification<StockMovement> occurredBefore(LocalDateTime to) {
    return (root, query, cb) ->
        to == null ? null : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
  }

  public static Specification<StockMovement> filter(
      Long productId, Long warehouseId, MovementType type, LocalDateTime from, LocalDateTime to) {
    return Specification.allOf(
        productIdEquals(productId),
        warehouseIdEquals(warehouseId),
        typeEquals(type),
        occurredAfter(from),
        occurredBefore(to));
  }
}
