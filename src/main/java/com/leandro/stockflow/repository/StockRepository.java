package com.leandro.stockflow.repository;

import com.leandro.stockflow.entity.Stock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface StockRepository extends JpaRepository<Stock, Long> {

  Optional<Stock> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT s FROM Stock s
      WHERE s.product.id = :productId AND s.warehouse.id = :warehouseId\
      """)
  Optional<Stock> findForUpdate(Long productId, Long warehouseId);

  List<Stock> findByProductId(Long productId);
}
