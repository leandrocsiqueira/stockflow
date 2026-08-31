package com.leandro.stockflow.entity;

import com.leandro.stockflow.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

@Entity
@Table(
    name = "stock",
    uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "warehouse_id"}))
public class Stock {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(nullable = false)
  private int quantity;

  @Version private long version;

  protected Stock() {}

  public Stock(Product product, Warehouse warehouse) {
    if (product == null) {
      throw new IllegalArgumentException("Product is required");
    }
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse is required");
    }
    this.product = product;
    this.warehouse = warehouse;
    this.quantity = 0;
  }

  public void increase(int amount) {
    validatePositiveAmount(amount);
    this.quantity += amount;
  }

  public void decrease(int amount) {
    validatePositiveAmount(amount);

    if (amount > this.quantity) {
      throw new BusinessRuleException(
          "Insufficient stock: available " + this.quantity + ", requested " + amount);
    }
    this.quantity -= amount;
  }

  public boolean isBelowMinimum() {
    return quantity < product.getMinimumStock();
  }

  public Long getId() {
    return id;
  }

  public Product getProduct() {
    return product;
  }

  public Warehouse getWarehouse() {
    return warehouse;
  }

  public int getQuantity() {
    return quantity;
  }

  public long getVersion() {
    return version;
  }

  private void validatePositiveAmount(int amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Stock amount must be positive");
    }
  }
}
