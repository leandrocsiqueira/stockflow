package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leandro.stockflow.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

class StockTest {
  private final Product product = new Product("SKU-001", "Keyboard", "UNIT", 10);
  private final Warehouse warehouse = new Warehouse("Main Warehouse", "Building A");

  @Test
  void shouldStartWithZeroQuantity() {
    Stock stock = new Stock(product, warehouse);
    assertThat(stock.getQuantity()).isZero();
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
