package com.leandro.stockflow.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WarehouseTest {

  @Test
  void shouldRejectBlankNameWhenCreatingWarehouse() {
    assertThatThrownBy(() -> new Warehouse("   ", "Main building"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Warehouse name is required");
  }

  @Test
  void shouldRejectBlankNameWhenUpdatingWarehouse() {
    Warehouse warehouse = new Warehouse("Main Warehouse", "Main building");

    assertThatThrownBy(() -> warehouse.setName("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Warehouse name is required");
  }
}
