package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leandro.stockflow.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class ReplenishmentOrderTest {

  private final Product product = new Product("SKU-001", "Keyboard", "UNIT");
  private final Warehouse warehouse = new Warehouse("Main Warehouse", "Building A");

  @Test
  void shouldCreatePendingReplenishmentOrder() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10);
    assertThat(order.getStatus()).isEqualTo(ReplenishmentStatus.PENDING);
    assertThat(order.getRequestedQuantity()).isEqualTo(10);
    assertThat(order.getCreatedAt()).isNotNull();
    assertThat(order.getCompletedAt()).isNull();
  }

  @Test
  void shouldRejectOrderWithoutProduct() {
    assertThatThrownBy(() -> new ReplenishmentOrder(null, warehouse, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product is required");
  }

  @Test
  void shouldRejectOrderWithoutWarehouse() {
    assertThatThrownBy(() -> new ReplenishmentOrder(product, null, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Warehouse is required");
  }

  @Test
  void shouldRejectZeroRequestedQuantity() {
    assertThatThrownBy(() -> new ReplenishmentOrder(product, warehouse, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requested quantity must be positive");
  }

  @Test
  void shouldRejectNegativeRequestedQuantity() {
    assertThatThrownBy(() -> new ReplenishmentOrder(product, warehouse, -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Requested quantity must be positive");
  }

  @Test
  void shouldCompletePendingOrder() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10);
    order.complete();
    assertThat(order.getStatus()).isEqualTo(ReplenishmentStatus.COMPLETED);
    assertThat(order.getCompletedAt()).isNotNull();
  }

  @Test
  void shouldRejectCompletingOrderTwice() {
    ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, 10);
    order.complete();
    assertThatThrownBy(order::complete)
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Only pending replenishment orders can be completed");
  }
}
