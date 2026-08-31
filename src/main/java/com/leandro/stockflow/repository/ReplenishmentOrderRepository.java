package com.leandro.stockflow.repository;

import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReplenishmentOrderRepository extends JpaRepository<ReplenishmentOrder, Long> {
  Optional<ReplenishmentOrder> findByProductIdAndWarehouseIdAndStatus(
      Long productId, Long warehouseId, ReplenishmentStatus status);

  List<ReplenishmentOrder> findByStatus(ReplenishmentStatus status);
}
