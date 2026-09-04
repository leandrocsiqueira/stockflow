package com.leandro.stockflow.repository;

import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ReplenishmentOrderRepository extends JpaRepository<ReplenishmentOrder, Long> {
  Optional<ReplenishmentOrder> findByProductIdAndWarehouseIdAndStatus(
      Long productId, Long warehouseId, ReplenishmentStatus status);

  List<ReplenishmentOrder> findByStatus(ReplenishmentStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM ReplenishmentOrder r WHERE r.id = :id")
  Optional<ReplenishmentOrder> findForUpdate(Long id);
}
