package com.leandro.stockflow.service;

import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.dto.StockResponse;
import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.entity.Product;
import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import com.leandro.stockflow.entity.Stock;
import com.leandro.stockflow.entity.StockMovement;
import com.leandro.stockflow.entity.Warehouse;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.mapper.StockMapper;
import com.leandro.stockflow.mapper.StockMovementMapper;
import com.leandro.stockflow.repository.ProductRepository;
import com.leandro.stockflow.repository.ReplenishmentOrderRepository;
import com.leandro.stockflow.repository.StockMovementRepository;
import com.leandro.stockflow.repository.StockRepository;
import com.leandro.stockflow.repository.WarehouseRepository;
import com.leandro.stockflow.specification.StockMovementSpecifications;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

  private final StockRepository stockRepository;
  private final StockMovementRepository movementRepository;
  private final ProductRepository productRepository;
  private final WarehouseRepository warehouseRepository;
  private final ReplenishmentOrderRepository replenishmentOrderRepository;
  private final Clock clock;

  public StockService(
      StockRepository stockRepository,
      StockMovementRepository movementRepository,
      ProductRepository productRepository,
      WarehouseRepository warehouseRepository,
      ReplenishmentOrderRepository replenishmentOrderRepository,
      Clock clock) {
    this.stockRepository = stockRepository;
    this.movementRepository = movementRepository;
    this.productRepository = productRepository;
    this.warehouseRepository = warehouseRepository;
    this.replenishmentOrderRepository = replenishmentOrderRepository;
    this.clock = clock;
  }

  @Transactional
  public StockMovementResponse registerMovement(StockMovementRequest request) {
    Product product =
        productRepository
            .findById(request.productId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Product not found with id: " + request.productId()));

    Warehouse warehouse =
        warehouseRepository
            .findById(request.warehouseId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Warehouse not found with id: " + request.warehouseId()));

    Stock stock =
        stockRepository
            .findForUpdate(product.getId(), warehouse.getId())
            .orElseGet(() -> stockRepository.save(new Stock(product, warehouse)));

    switch (request.type()) {
      case IN -> stock.increase(request.quantity());
      case OUT -> stock.decrease(request.quantity());
    }

    StockMovement movement =
        new StockMovement(
            product,
            warehouse,
            request.type(),
            request.quantity(),
            request.reason(),
            request.reference(),
            LocalDateTime.now(clock));
    movementRepository.save(movement);

    if (stock.isBelowMinimum()) {
      triggerReplenishmentIfNeeded(product, warehouse, stock);
    }

    return StockMovementMapper.toResponse(movement);
  }

  private void triggerReplenishmentIfNeeded(Product product, Warehouse warehouse, Stock stock) {
    boolean alreadyPending =
        replenishmentOrderRepository
            .findByProductIdAndWarehouseIdAndStatus(
                product.getId(), warehouse.getId(), ReplenishmentStatus.PENDING)
            .isPresent();

    if (!alreadyPending) {
      int deficit = product.getMinimumStock() - stock.getQuantity();
      ReplenishmentOrder order = new ReplenishmentOrder(product, warehouse, deficit);
      replenishmentOrderRepository.save(order);
    }
  }

  @Transactional(readOnly = true)
  public StockResponse getBalance(Long productId, Long warehouseId) {
    Stock stock =
        stockRepository
            .findByProductIdAndWarehouseId(productId, warehouseId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "No stock record for product "
                            + productId
                            + " in warehouse "
                            + warehouseId));
    return StockMapper.toResponse(stock);
  }

  @Transactional(readOnly = true)
  public List<StockResponse> getBalancesForProduct(Long productId) {
    return stockRepository.findByProductId(productId).stream()
        .map(StockMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Page<StockMovementResponse> findMovements(
      Long productId,
      Long warehouseId,
      MovementType type,
      LocalDateTime from,
      LocalDateTime to,
      Pageable pageable) {
    var spec = StockMovementSpecifications.filter(productId, warehouseId, type, from, to);
    return movementRepository.findAll(spec, pageable).map(StockMovementMapper::toResponse);
  }
}
