package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leandro.stockflow.exception.BusinessRuleException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ReplenishmentOrderTest {

  private final Product product = new Product("SKU-001", "Keyboard", "UNIT");
  private final Warehouse warehouse = new Warehouse("Main Warehouse", "Building A");
  private final LocalDateTime createdAt = LocalDateTime.of(2026, 9, 4, 9, 0);

  @Test
  void shouldCreatePendingReplenishmentOrder() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10, createdAt);

    assertThat(order.getStatus()).isEqualTo(ReplenishmentStatus.PENDING);
    assertThat(order.getRequestedQuantity()).isEqualTo(10);
    assertThat(order.getCreatedAt()).isEqualTo(createdAt);
    assertThat(order.getCompletedAt()).isNull();
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void shouldRejectOrderWithoutProduct() {
    assertThatThrownBy(() -> new ReplenishmentOrder(null, warehouse, 10, createdAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product is required");
  }

  @Test
  void shouldRejectOrderWithoutWarehouse() {
    assertThatThrownBy(() -> new ReplenishmentOrder(product, null, 10, createdAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Warehouse is required");
  }

  @Test
  void shouldRejectZeroRequestedQuantity() {
    assertThatThrownBy(() -> new ReplenishmentOrder(product, warehouse, 0, createdAt))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requested quantity must be positive");
  }

  @Test
  void shouldRejectMissingCreatedTimestamp() {
    assertThatThrownBy(() -> new ReplenishmentOrder(product, warehouse, 10, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Created timestamp is required");
  }

  @Test
  void shouldCompletePendingOrder() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10, createdAt);
    LocalDateTime completedAt = createdAt.plusHours(2);

    order.complete(completedAt);

    assertThat(order.getStatus()).isEqualTo(ReplenishmentStatus.COMPLETED);
    assertThat(order.getCompletedAt()).isEqualTo(completedAt);
    assertThat(order.getCancelledAt()).isNull();
  }

  @Test
  void shouldCancelPendingOrder() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10, createdAt);
    LocalDateTime cancelledAt = createdAt.plusHours(1);

    order.cancel(cancelledAt);

    assertThat(order.getStatus()).isEqualTo(ReplenishmentStatus.CANCELLED);
    assertThat(order.getCancelledAt()).isEqualTo(cancelledAt);
    assertThat(order.getCompletedAt()).isNull();
  }

  @Test
  void shouldRejectCompletingOrderTwice() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10, createdAt);
    order.complete(createdAt.plusHours(1));

    assertThatThrownBy(() -> order.complete(createdAt.plusHours(2)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Only pending replenishment orders can be completed");
  }

  @Test
  void shouldRejectCancellingCompletedOrder() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10, createdAt);
    order.complete(createdAt.plusHours(1));

    assertThatThrownBy(() -> order.cancel(createdAt.plusHours(2)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Only pending replenishment orders can be cancelled");
  }
}
