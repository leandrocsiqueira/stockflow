package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class StockMovementTest {

  private final Product product = new Product("SKU-001", "Keyboard", "UNIT");
  private final Warehouse warehouse = new Warehouse("Main Warehouse", "Building A");

  @Test
  void shouldRejectMovementWithoutProduct() {
    assertThatThrownBy(
            () ->
                new StockMovement(
                    null, warehouse, MovementType.IN, 10, "purchase", "PO-1", LocalDateTime.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product is required");
  }

  @Test
  void shouldRejectMovementWithoutWarehouse() {
    assertThatThrownBy(
            () ->
                new StockMovement(
                    product, null, MovementType.IN, 10, "purchase", "PO-1", LocalDateTime.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Warehouse is required");
  }

  @Test
  void shouldRejectMovementWithoutType() {
    assertThatThrownBy(
            () ->
                new StockMovement(
                    product, warehouse, null, 10, "purchase", "PO-1", LocalDateTime.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Movement type is required");
  }

  @Test
  void shouldRejectMovementWithZeroQuantity() {
    assertThatThrownBy(
            () ->
                new StockMovement(
                    product,
                    warehouse,
                    MovementType.IN,
                    0,
                    "purchase",
                    "PO-1",
                    LocalDateTime.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Movement quantity must be positive");
  }

  @Test
  void shouldRejectMovementWithNegativeQuantity() {
    assertThatThrownBy(
            () ->
                new StockMovement(
                    product,
                    warehouse,
                    MovementType.IN,
                    -1,
                    "purchase",
                    "PO-1",
                    LocalDateTime.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Movement quantity must be positive");
  }

  @Test
  void shouldRejectMovementWithoutOccurrenceDate() {
    assertThatThrownBy(
            () ->
                new StockMovement(
                    product, warehouse, MovementType.IN, 10, "purchase", "PO-1", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Occurrence date is required");
  }
}
