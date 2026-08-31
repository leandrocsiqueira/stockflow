package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductTest {

  @Test
  void shouldRejectNegativeMinimumStock() {
    assertThatThrownBy(() -> new Product("SKU-001", "Keyboard", "UNIT", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Minimum stock cannot be negative");
  }

  @Test
  void shouldRejectNegativeMinimumStockWhenUpdatingProduct() {
    Product product = new Product("SKU-001", "Keyboard", "UNIT", 10);
    assertThatThrownBy(() -> product.setMinimumStock(-1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Minimum stock cannot be negative");
  }

  @Test
  void shouldRejectBlankNameWhenCreatingProduct() {
    assertThatThrownBy(() -> new Product("SKU-001", "   ", "UNIT", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product name is required");
  }

  @Test
  void shouldRejectBlankNameWhenUpdatingProduct() {
    Product product = new Product("SKU-001", "Keyboard", "UNIT", 10);

    assertThatThrownBy(() -> product.setName("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product name is required");
  }

  @Test
  void shouldRejectBlankSkuWhenCreatingProduct() {
    assertThatThrownBy(() -> new Product("   ", "Keyboard", "UNIT", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("SKU is required");
  }

  @Test
  void shouldRejectBlankUnitWhenCreatingProduct() {
    assertThatThrownBy(() -> new Product("SKU-001", "Keyboard", "   ", 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unit is required");
  }
}
