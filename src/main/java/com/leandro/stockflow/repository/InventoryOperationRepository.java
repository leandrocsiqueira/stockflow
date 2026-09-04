package com.leandro.stockflow.repository;

import com.leandro.stockflow.entity.InventoryOperation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryOperationRepository extends JpaRepository<InventoryOperation, Long> {

  Optional<InventoryOperation> findByIdempotencyKey(String idempotencyKey);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO inventory_operations (idempotency_key, operation_type, request_hash)
          VALUES (:idempotencyKey, :operationType, :requestHash)
          ON CONFLICT (idempotency_key) DO NOTHING
          """,
      nativeQuery = true)
  int reserve(
      @Param("idempotencyKey") String idempotencyKey,
      @Param("operationType") String operationType,
      @Param("requestHash") String requestHash);
}
