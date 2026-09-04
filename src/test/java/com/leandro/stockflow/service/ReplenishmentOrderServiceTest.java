package com.leandro.stockflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leandro.stockflow.entity.Product;
import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import com.leandro.stockflow.entity.Stock;
import com.leandro.stockflow.entity.StockMovement;
import com.leandro.stockflow.entity.Warehouse;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.repository.ReplenishmentOrderRepository;
import com.leandro.stockflow.repository.StockMovementRepository;
import com.leandro.stockflow.repository.StockRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReplenishmentOrderServiceTest {

  @Mock private ReplenishmentOrderRepository replenishmentOrderRepository;
  @Mock private StockRepository stockRepository;
  @Mock private StockMovementRepository movementRepository;

  private ReplenishmentOrderService replenishmentOrderService;
  private Product product;
  private Warehouse warehouse;
  private LocalDateTime now;

  @BeforeEach
  void setUp() {
    Clock clock = Clock.fixed(Instant.parse("2026-09-04T13:00:00Z"), ZoneOffset.UTC);
    now = LocalDateTime.of(2026, 9, 4, 13, 0);
    replenishmentOrderService =
        new ReplenishmentOrderService(
            replenishmentOrderRepository, stockRepository, movementRepository, clock);

    product = new Product("SKU-001", "Keyboard", "UN");
    warehouse = new Warehouse("Main Warehouse", "SP");
    ReflectionTestUtils.setField(product, "id", 1L);
    ReflectionTestUtils.setField(warehouse, "id", 2L);
  }

  @Test
  void shouldCancelPendingOrder() {
    ReplenishmentOrder order = pendingOrder();
    when(replenishmentOrderRepository.findForUpdate(7L)).thenReturn(Optional.of(order));

    var response = replenishmentOrderService.cancel(7L);

    assertThat(response.status()).isEqualTo(ReplenishmentStatus.CANCELLED);
    assertThat(response.cancelledAt()).isEqualTo(now);
  }

  @Test
  void shouldReceivePendingOrderIntoStock() {
    ReplenishmentOrder order = pendingOrder();
    Stock stock = new Stock(product, warehouse);
    stock.configurePolicy(10, 30);
    stock.increase(5);

    when(replenishmentOrderRepository.findForUpdate(7L)).thenReturn(Optional.of(order));
    when(stockRepository.findForUpdate(1L, 2L)).thenReturn(Optional.of(stock));
    when(movementRepository.save(any(StockMovement.class)))
        .thenAnswer(
            invocation -> {
              StockMovement movement = invocation.getArgument(0);
              ReflectionTestUtils.setField(movement, "id", 99L);
              return movement;
            });

    var response = replenishmentOrderService.receive(7L);

    assertThat(response.orderId()).isEqualTo(7L);
    assertThat(response.movementId()).isEqualTo(99L);
    assertThat(response.receivedQuantity()).isEqualTo(25);
    assertThat(response.resultingBalance()).isEqualTo(30);
    assertThat(response.reference()).isEqualTo("REPLENISHMENT-7");
    assertThat(order.getStatus()).isEqualTo(ReplenishmentStatus.COMPLETED);
    assertThat(order.getCompletedAt()).isEqualTo(now);
    verify(replenishmentOrderRepository).flush();
  }

  @Test
  void shouldRejectReceivingCancelledOrderBeforeChangingStock() {
    ReplenishmentOrder order = pendingOrder();
    order.cancel(now.minusMinutes(10));
    when(replenishmentOrderRepository.findForUpdate(7L)).thenReturn(Optional.of(order));

    assertThatThrownBy(() -> replenishmentOrderService.receive(7L))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Only pending replenishment orders can be received");

    verify(stockRepository, never()).findForUpdate(any(), any());
    verify(movementRepository, never()).save(any());
  }

  private ReplenishmentOrder pendingOrder() {
    ReplenishmentOrder order =
        new ReplenishmentOrder(product, warehouse, 25, now.minusHours(1));
    ReflectionTestUtils.setField(order, "id", 7L);
    return order;
  }
}
