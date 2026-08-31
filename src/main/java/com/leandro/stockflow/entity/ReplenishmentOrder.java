package com.leandro.stockflow.entity;

import com.leandro.stockflow.exception.BusinessRuleException;
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
@Table(name = "replenishment_orders")
public class ReplenishmentOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "warehouse_id", nullable = false)
  private Warehouse warehouse;

  @Column(name = "requested_quantity", nullable = false)
  private int requestedQuantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReplenishmentStatus status;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  protected ReplenishmentOrder() {}

  public ReplenishmentOrder(Product product, Warehouse warehouse, int requestedQuantity) {
    if (product == null) {
      throw new IllegalArgumentException("Product is required");
    }
    if (warehouse == null) {
      throw new IllegalArgumentException("Warehouse is required");
    }
    if (requestedQuantity <= 0) {
      throw new IllegalArgumentException("Requested quantity must be positive");
    }
    this.product = product;
    this.warehouse = warehouse;
    this.requestedQuantity = requestedQuantity;
    this.status = ReplenishmentStatus.PENDING;
    this.createdAt = LocalDateTime.now();
  }

  public void complete() {
    if (status != ReplenishmentStatus.PENDING) {
      throw new BusinessRuleException("Only pending replenishment orders can be completed");
    }
    this.status = ReplenishmentStatus.COMPLETED;
    this.completedAt = LocalDateTime.now();
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

  public int getRequestedQuantity() {
    return requestedQuantity;
  }

  public ReplenishmentStatus getStatus() {
    return status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }
}
