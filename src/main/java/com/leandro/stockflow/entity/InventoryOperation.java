package com.leandro.stockflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_operations")
public class InventoryOperation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
  private String idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "operation_type", nullable = false, length = 32)
  private InventoryOperationType operationType;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "response_body", columnDefinition = "text")
  private String responseBody;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  protected InventoryOperation() {}

  public Long getId() {
    return id;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public InventoryOperationType getOperationType() {
    return operationType;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void complete(String responseBody) {
    if (responseBody == null || responseBody.isBlank()) {
      throw new IllegalArgumentException("Idempotency response body is required");
    }
    this.responseBody = responseBody;
  }
}
