package com.leandro.stockflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MovementType type;

  @Column(nullable = false)
  private int quantity;

  private String reason;

  private String reference;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  protected StockMovement() {}

  public StockMovement(
      Product product,
      Warehouse warehouse,
      MovementType type,
      int quantity,
      String reason,
      String reference,
      LocalDateTime occurredAt) {
    if (product == null) {
      throw new IllegalArgumentException("Product is required");
    }
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse is required");
    }
    if (type == null) {
      throw new IllegalArgumentException("Movement type is required");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("Movement quantity must be positive");
    }
    if (occurredAt == null) {
      throw new IllegalArgumentException("Occurrence date is required");
    }
    this.product = product;
    this.warehouse = warehouse;
    this.type = type;
    this.quantity = quantity;
    this.reason = reason;
    this.reference = reference;
    this.occurredAt = occurredAt;
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

  public MovementType getType() {
    return type;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getReason() {
    return reason;
  }

  public String getReference() {
    return reference;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }
}
