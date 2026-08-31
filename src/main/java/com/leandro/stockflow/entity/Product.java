package com.leandro.stockflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String sku;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String unit;

  @Column(name = "minimum_stock", nullable = false)
  private int minimumStock;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  protected Product() {}

  public Product(String sku, String name, String unit, int minimumStock) {
    this.sku = validateSku(sku);
    setName(name);
    this.unit = validateUnit(unit);
    setMinimumStock(minimumStock);
    this.createdAt = LocalDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Product name is required");
    }
    this.name = name;
  }

  public String getUnit() {
    return unit;
  }

  public int getMinimumStock() {
    return minimumStock;
  }

  public void setMinimumStock(int minimumStock) {
    if (minimumStock < 0) {
      throw new IllegalArgumentException("Minimum stock cannot be negative");
    }
    this.minimumStock = minimumStock;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  private String validateSku(String sku) {
    if (sku == null || sku.isBlank()) {
      throw new IllegalArgumentException("SKU is required");
    }
    return sku;
  }

  private String validateUnit(String unit) {
    if (unit == null || unit.isBlank()) {
      throw new IllegalArgumentException("Unit is required");
    }
    return unit;
  }
}
