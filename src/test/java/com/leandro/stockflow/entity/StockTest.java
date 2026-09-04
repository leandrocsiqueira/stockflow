package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leandro.stockflow.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class StockTest {
  private final Product product = new Product("SKU-001", "Keyboard", "UNIT");
  private final Warehouse warehouse = new Warehouse("Main Warehouse", "Building A");

  @Test
  void shouldStartWithZeroQuantityAndDisabledReplenishmentPolicy() {
    Stock stock = new Stock(product, warehouse);

    assertThat(stock.getQuantity()).isZero();
    assertThat(stock.getReorderPoint()).isZero();
    assertThat(stock.getTargetStock()).isZero();
    assertThat(stock.isBelowReorderPoint()).isFalse();
  }

  @Test
  void shouldConfigureInventoryPolicy() {
    Stock stock = new Stock(product, warehouse);

    stock.configurePolicy(10, 30);

    assertThat(stock.getReorderPoint()).isEqualTo(10);
    assertThat(stock.getTargetStock()).isEqualTo(30);
    assertThat(stock.isBelowReorderPoint()).isTrue();
    assertThat(stock.replenishmentQuantity()).isEqualTo(30);
  }

  @Test
  void shouldCalculateReplenishmentQuantityUsingTargetStock() {
    Stock stock = new Stock(product, warehouse);
    stock.configurePolicy(10, 30);
    stock.increase(15);
    stock.decrease(6);

    assertThat(stock.getQuantity()).isEqualTo(9);
    assertThat(stock.isBelowReorderPoint()).isTrue();
    assertThat(stock.replenishmentQuantity()).isEqualTo(21);
  }

  @Test
  void shouldRejectNegativeReorderPoint() {
    Stock stock = new Stock(product, warehouse);

    assertThatThrownBy(() -> stock.configurePolicy(-1, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Reorder point cannot be negative");
  }

  @Test
  void shouldRejectTargetStockBelowReorderPoint() {
    Stock stock = new Stock(product, warehouse);

    assertThatThrownBy(() -> stock.configurePolicy(10, 9))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Target stock must be greater than or equal to reorder point");
  }

  @Test
  void shouldIncreaseStock() {
    Stock stock = new Stock(product, warehouse);
    stock.increase(10);
    assertThat(stock.getQuantity()).isEqualTo(10);
  }

  @Test
  void shouldRejectZeroAmountWhenIncreasingStock() {
    Stock stock = new Stock(product, warehouse);
    assertThatThrownBy(() -> stock.increase(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Stock amount must be positive");
  }

  @Test
  void shouldRejectNegativeAmountWhenIncreasingStock() {
    Stock stock = new Stock(product, warehouse);
    assertThatThrownBy(() -> stock.increase(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Stock amount must be positive");
  }

  @Test
  void shouldDecreaseStock() {
    Stock stock = new Stock(product, warehouse);
    stock.increase(20);
    stock.decrease(5);
    assertThat(stock.getQuantity()).isEqualTo(15);
  }

  @Test
  void shouldRejectZeroAmountWhenDecreasingStock() {
    Stock stock = new Stock(product, warehouse);
    stock.increase(20);
    assertThatThrownBy(() -> stock.decrease(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Stock amount must be positive");
  }

  @Test
  void shouldRejectNegativeAmountWhenDecreasingStock() {
    Stock stock = new Stock(product, warehouse);
    stock.increase(20);
    assertThatThrownBy(() -> stock.decrease(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Stock amount must be positive");
  }

  @Test
  void shouldRejectDecreaseWhenStockIsInsufficient() {
    Stock stock = new Stock(product, warehouse);
    stock.increase(10);
    assertThatThrownBy(() -> stock.decrease(11))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Insufficient stock: available 10, requested 11");
    assertThat(stock.getQuantity()).isEqualTo(10);
  }

  @Test
  void shouldRejectStockWithoutProduct() {
    assertThatThrownBy(() -> new Stock(null, warehouse))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product is required");
  }

  @Test
  void shouldRejectStockWithoutWarehouse() {
    assertThatThrownBy(() -> new Stock(product, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Warehouse is required");
  }
}
