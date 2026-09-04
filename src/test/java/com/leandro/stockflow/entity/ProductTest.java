package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductTest {

  @Test
  void shouldRejectBlankNameWhenCreatingProduct() {
    assertThatThrownBy(() -> new Product("SKU-001", "   ", "UNIT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product name is required");
  }

  @Test
  void shouldRejectBlankNameWhenUpdatingProduct() {
    Product product = new Product("SKU-001", "Keyboard", "UNIT");

    assertThatThrownBy(() -> product.setName("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Product name is required");
  }

  @Test
  void shouldRejectBlankSkuWhenCreatingProduct() {
    assertThatThrownBy(() -> new Product("   ", "Keyboard", "UNIT"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("SKU is required");
  }

  @Test
  void shouldRejectBlankUnitWhenCreatingProduct() {
    assertThatThrownBy(() -> new Product("SKU-001", "Keyboard", "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unit is required");
  }
}
