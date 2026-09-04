package com.leandro.stockflow.service;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
import com.leandro.stockflow.dto.ReplenishmentReceiptResponse;
import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import com.leandro.stockflow.entity.Stock;
import com.leandro.stockflow.entity.StockMovement;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.mapper.ReplenishmentOrderMapper;
import com.leandro.stockflow.repository.ReplenishmentOrderRepository;
import com.leandro.stockflow.repository.StockMovementRepository;
import com.leandro.stockflow.repository.StockRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReplenishmentOrderService {

  private static final String RECEIPT_REASON = "Replenishment order received";

  private final ReplenishmentOrderRepository replenishmentOrderRepository;
  private final StockRepository stockRepository;
  private final StockMovementRepository movementRepository;
  private final Clock clock;

  public ReplenishmentOrderService(
      ReplenishmentOrderRepository replenishmentOrderRepository,
      StockRepository stockRepository,
      StockMovementRepository movementRepository,
      Clock clock) {
    this.replenishmentOrderRepository = replenishmentOrderRepository;
    this.stockRepository = stockRepository;
    this.movementRepository = movementRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<ReplenishmentOrderResponse> findPending() {
    return replenishmentOrderRepository.findByStatus(ReplenishmentStatus.PENDING).stream()
        .map(ReplenishmentOrderMapper::toResponse)
        .toList();
  }

  @Transactional
  public ReplenishmentOrderResponse cancel(Long id) {
    ReplenishmentOrder order = findForUpdate(id);
    order.cancel(LocalDateTime.now(clock));
    return ReplenishmentOrderMapper.toResponse(order);
  }

  @Transactional
  public ReplenishmentReceiptResponse receive(Long id) {
    ReplenishmentOrder order = findForUpdate(id);
    if (order.getStatus() != ReplenishmentStatus.PENDING) {
      throw new BusinessRuleException("Only pending replenishment orders can be received");
    }

    Stock stock =
        stockRepository
            .findForUpdate(order.getProduct().getId(), order.getWarehouse().getId())
            .orElseGet(
                () ->
                    stockRepository.save(
                        new Stock(order.getProduct(), order.getWarehouse())));

    LocalDateTime receivedAt = LocalDateTime.now(clock);
    stock.increase(order.getRequestedQuantity());

    String reference = "REPLENISHMENT-" + order.getId();
    StockMovement movement =
        movementRepository.save(
            new StockMovement(
                order.getProduct(),
                order.getWarehouse(),
                MovementType.IN,
                order.getRequestedQuantity(),
                RECEIPT_REASON,
                reference,
                receivedAt));

    order.complete(receivedAt);
    replenishmentOrderRepository.flush();

    if (stock.isBelowReorderPoint()) {
      ReplenishmentOrder followUp =
          new ReplenishmentOrder(
              order.getProduct(),
              order.getWarehouse(),
              stock.replenishmentQuantity(),
              receivedAt);
      replenishmentOrderRepository.save(followUp);
    }

    return new ReplenishmentReceiptResponse(
        order.getId(),
        movement.getId(),
        order.getProduct().getSku(),
        order.getProduct().getName(),
        order.getWarehouse().getName(),
        order.getRequestedQuantity(),
        stock.getQuantity(),
        reference,
        receivedAt);
  }

  private ReplenishmentOrder findForUpdate(Long id) {
    return replenishmentOrderRepository
        .findForUpdate(id)
        .orElseThrow(
            () -> new ResourceNotFoundException("Replenishment order not found with id: " + id));
  }
}
