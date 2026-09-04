package com.leandro.stockflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leandro.stockflow.dto.CreateProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.dto.StockPolicyRequest;
import com.leandro.stockflow.dto.StockResponse;
import com.leandro.stockflow.dto.StockTransferRequest;
import com.leandro.stockflow.dto.StockTransferResponse;
import com.leandro.stockflow.dto.CreateWarehouseRequest;
import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.entity.Product;
import com.leandro.stockflow.entity.ReplenishmentOrder;
import com.leandro.stockflow.entity.Warehouse;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.repository.ProductRepository;
import com.leandro.stockflow.repository.ReplenishmentOrderRepository;
import com.leandro.stockflow.repository.WarehouseRepository;
import com.leandro.stockflow.service.ProductService;
import com.leandro.stockflow.service.ReplenishmentOrderService;
import com.leandro.stockflow.service.StockService;
import com.leandro.stockflow.service.WarehouseService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class StockMovementIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("stockflow_test")
          .withUsername("stockflow")
          .withPassword("stockflow");

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private ProductService productService;
  @Autowired private WarehouseService warehouseService;
  @Autowired private StockService stockService;
  @Autowired private ReplenishmentOrderService replenishmentOrderService;
  @Autowired private ProductRepository productRepository;
  @Autowired private WarehouseRepository warehouseRepository;
  @Autowired private ReplenishmentOrderRepository replenishmentOrderRepository;

  private Long productId;
  private Long warehouseId;

  @BeforeEach
  void setUp() {
    ProductResponse product =
        productService.create(
            new CreateProductRequest("SKU-" + System.nanoTime(), "Test Widget", "unit"));
    WarehouseResponse warehouse =
        warehouseService.create(new CreateWarehouseRequest("Main Warehouse", "SP"));
    productId = product.id();
    warehouseId = warehouse.id();
  }

  @Test
  void shouldAccumulateStockAcrossMultipleInMovements() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 50, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 30, "purchase", "PO-2"));
    StockResponse balance = stockService.getBalance(productId, warehouseId);
    assertThat(balance.quantity()).isEqualTo(80);
  }

  @Test
  void shouldDecreaseStockOnOutMovementAndKeepBalanceConsistent() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 100, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 40, "sale", "SO-1"));
    StockResponse balance = stockService.getBalance(productId, warehouseId);
    assertThat(balance.quantity()).isEqualTo(60);
  }

  @Test
  void shouldRejectOutMovementExceedingAvailableBalance() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-1"));
    assertThatThrownBy(
            () ->
                registerMovement(
                    new StockMovementRequest(
                        productId, warehouseId, MovementType.OUT, 20, "sale", "SO-1")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Insufficient stock");
    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(10);
    var movements =
        stockService.findMovements(
            productId, warehouseId, null, null, null, PageRequest.of(0, 100));
    assertThat(movements.getContent()).hasSize(1);
    assertThat(movements.getContent().getFirst().type()).isEqualTo(MovementType.IN);
    assertThat(movements.getContent().getFirst().quantity()).isEqualTo(10);
  }

  @Test
  void shouldTriggerReplenishmentToTargetStockWhenBalanceDropsBelowReorderPoint() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-1"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-1"));

    var productSku = productRepository.findById(productId).orElseThrow().getSku();
    var pending = replenishmentOrderService.findPending();

    assertThat(pending)
        .anyMatch(
            order ->
                order.productSku().equals(productSku)
                    && order.warehouseName().equals("Main Warehouse")
                    && order.requestedQuantity() == 25
                    && order.status().name().equals("PENDING"));
  }

  @Test
  void shouldNotDuplicateReplenishmentOrderWhileOnePending() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-1"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-1"));

    long firstCount = replenishmentOrderService.findPending().size();

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 1, "purchase", "PO-2"));

    long secondCount = replenishmentOrderService.findPending().size();
    assertThat(secondCount).isEqualTo(firstCount);
  }

  @Test
  void shouldKeepInventoryPolicySpecificToEachWarehouse() {
    WarehouseResponse secondaryWarehouse =
        warehouseService.create(new CreateWarehouseRequest("Secondary Warehouse", "MG"));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(
            productId, secondaryWarehouse.id(), MovementType.IN, 10, "purchase", "PO-2"));

    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));
    stockService.configurePolicy(new StockPolicyRequest(productId, secondaryWarehouse.id(), 5, 12));

    StockResponse mainBalance = stockService.getBalance(productId, warehouseId);
    StockResponse secondaryBalance =
        stockService.getBalance(productId, secondaryWarehouse.id());

    assertThat(mainBalance.reorderPoint()).isEqualTo(10);
    assertThat(mainBalance.targetStock()).isEqualTo(30);
    assertThat(secondaryBalance.reorderPoint()).isEqualTo(5);
    assertThat(secondaryBalance.targetStock()).isEqualTo(12);
  }

  @Test
  void shouldTransferStockAtomicallyBetweenWarehouses() {
    WarehouseResponse secondaryWarehouse =
        warehouseService.create(new CreateWarehouseRequest("Transfer Destination", "MG"));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 50, "purchase", "PO-T1"));

    StockTransferResponse transfer =
        transfer(
            new StockTransferRequest(
                productId,
                warehouseId,
                secondaryWarehouse.id(),
                20,
                "Internal transfer",
                "TRF-001"));

    assertThat(transfer.sourceBalance()).isEqualTo(30);
    assertThat(transfer.destinationBalance()).isEqualTo(20);
    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(30);
    assertThat(stockService.getBalance(productId, secondaryWarehouse.id()).quantity()).isEqualTo(20);

    var transferMovements =
        stockService.findMovements(productId, null, null, null, null, PageRequest.of(0, 100)).stream()
            .filter(movement -> "TRF-001".equals(movement.reference()))
            .toList();

    assertThat(transferMovements).hasSize(2);
    assertThat(transferMovements)
        .extracting(StockMovementResponse::type)
        .containsExactlyInAnyOrder(MovementType.OUT, MovementType.IN);
    assertThat(transferMovements)
        .extracting(StockMovementResponse::quantity)
        .containsOnly(20);
  }

  @Test
  void shouldRollbackTransferWhenSourceStockIsInsufficient() {
    WarehouseResponse secondaryWarehouse =
        warehouseService.create(new CreateWarehouseRequest("Rollback Destination", "PR"));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-T2"));
    stockService.configurePolicy(new StockPolicyRequest(productId, secondaryWarehouse.id(), 0, 0));

    assertThatThrownBy(
            () ->
                transfer(
                    new StockTransferRequest(
                        productId,
                        warehouseId,
                        secondaryWarehouse.id(),
                        20,
                        "Invalid transfer",
                        "TRF-ROLLBACK")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Insufficient stock");

    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(10);
    assertThat(stockService.getBalance(productId, secondaryWarehouse.id()).quantity()).isZero();

    var transferMovements =
        stockService.findMovements(productId, null, null, null, null, PageRequest.of(0, 100)).stream()
            .filter(movement -> "TRF-ROLLBACK".equals(movement.reference()))
            .toList();
    assertThat(transferMovements).isEmpty();
  }

  @Test
  void shouldRejectTransferToSameWarehouseWithoutChangingStock() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 25, "purchase", "PO-T3"));

    assertThatThrownBy(
            () ->
                transfer(
                    new StockTransferRequest(
                        productId,
                        warehouseId,
                        warehouseId,
                        5,
                        "Invalid transfer",
                        "TRF-SAME")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Source and destination warehouses must be different");

    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(25);
  }

  @Test
  void shouldTriggerSourceReplenishmentAfterTransfer() {
    WarehouseResponse secondaryWarehouse =
        warehouseService.create(new CreateWarehouseRequest("Replenishment Destination", "SC"));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 50, "purchase", "PO-T4"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 40, 100));

    transfer(
        new StockTransferRequest(
            productId,
            warehouseId,
            secondaryWarehouse.id(),
            20,
            "Internal transfer",
            "TRF-REPLENISH"));

    String productSku = productRepository.findById(productId).orElseThrow().getSku();
    assertThat(replenishmentOrderService.findPending())
        .anyMatch(
            order ->
                order.productSku().equals(productSku)
                    && order.warehouseName().equals("Main Warehouse")
                    && order.requestedQuantity() == 70);
  }

  @Test
  void shouldFilterMovementHistoryByTypeUsingSpecification() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 50, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 5, "sale", "SO-2"));
    var outMovements =
        stockService.findMovements(
            productId, warehouseId, MovementType.OUT, null, null, PageRequest.of(0, 10));
    assertThat(outMovements.getContent()).hasSize(2);
    assertThat(outMovements.getContent()).allMatch(m -> m.type() == MovementType.OUT);
  }

  @Test
  void shouldKeepBalanceConsistentWithMovementHistory() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 100, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 30, "sale", "SO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 20, "sale", "SO-2"));
    StockResponse balance = stockService.getBalance(productId, warehouseId);
    var movements =
        stockService.findMovements(
            productId, warehouseId, null, null, null, PageRequest.of(0, 100));
    int calculatedBalance =
        movements.getContent().stream()
            .mapToInt(
                movement ->
                    movement.type() == MovementType.IN ? movement.quantity() : -movement.quantity())
            .sum();
    assertThat(calculatedBalance).isEqualTo(balance.quantity());
    assertThat(balance.quantity()).isEqualTo(50);
  }

  @Test
  void shouldRejectDuplicatePendingReplenishmentOrderAtDatabaseLevel() {
    Product product = productRepository.findById(productId).orElseThrow();
    Warehouse warehouse = warehouseRepository.findById(warehouseId).orElseThrow();
    ReplenishmentOrder first = new ReplenishmentOrder(product, warehouse, 10);
    replenishmentOrderRepository.saveAndFlush(first);
    ReplenishmentOrder duplicate = new ReplenishmentOrder(product, warehouse, 20);
    assertThatThrownBy(() -> replenishmentOrderRepository.saveAndFlush(duplicate))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void shouldCombineMovementFiltersUsingSpecification() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 100, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 20, "sale", "SO-1"));
    var movements =
        stockService.findMovements(
            productId, warehouseId, MovementType.OUT, null, null, PageRequest.of(0, 10));
    assertThat(movements.getContent()).hasSize(1);
    assertThat(movements.getContent().getFirst().type()).isEqualTo(MovementType.OUT);
    assertThat(movements.getContent().getFirst().quantity()).isEqualTo(20);
    assertThat(movements.getContent().getFirst().reference()).isEqualTo("SO-1");
  }

  @Test
  void shouldPaginateMovementHistory() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-1"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 20, "purchase", "PO-2"));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 30, "purchase", "PO-3"));
    var page =
        stockService.findMovements(productId, warehouseId, null, null, null, PageRequest.of(0, 2));
    assertThat(page.getContent()).hasSize(2);
    assertThat(page.getTotalElements()).isEqualTo(3);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(page.getNumber()).isZero();
    assertThat(page.getSize()).isEqualTo(2);
  }

  @Test
  void shouldSortMovementHistoryByQuantityDescending() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-1"));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 30, "purchase", "PO-2"));

    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 20, "purchase", "PO-3"));

    var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "quantity"));

    var page = stockService.findMovements(productId, warehouseId, null, null, null, pageable);

    assertThat(page.getContent())
        .extracting(StockMovementResponse::quantity)
        .containsExactly(30, 20, 10);
  }

  @Test
  void shouldReceiveReplenishmentIntoStockAndCompleteOrder() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-R1"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-R1"));

    String productSku = productRepository.findById(productId).orElseThrow().getSku();
    var pending =
        replenishmentOrderService.findPending().stream()
            .filter(
                order ->
                    order.productSku().equals(productSku)
                        && order.warehouseName().equals("Main Warehouse"))
            .findFirst()
            .orElseThrow();

    var receipt = replenishmentOrderService.receive(pending.id());

    assertThat(receipt.receivedQuantity()).isEqualTo(25);
    assertThat(receipt.resultingBalance()).isEqualTo(30);
    assertThat(receipt.reference()).isEqualTo("REPLENISHMENT-" + pending.id());
    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(30);

    ReplenishmentOrder completed = replenishmentOrderRepository.findById(pending.id()).orElseThrow();
    assertThat(completed.getStatus().name()).isEqualTo("COMPLETED");
    assertThat(completed.getCompletedAt()).isNotNull();
    assertThat(completed.getCancelledAt()).isNull();

    var receiptMovements =
        stockService.findMovements(productId, warehouseId, null, null, null, PageRequest.of(0, 100))
            .stream()
            .filter(movement -> receipt.reference().equals(movement.reference()))
            .toList();

    assertThat(receiptMovements).hasSize(1);
    assertThat(receiptMovements.getFirst().type()).isEqualTo(MovementType.IN);
    assertThat(receiptMovements.getFirst().quantity()).isEqualTo(25);
  }

  @Test
  void shouldCancelPendingReplenishmentWithoutChangingStock() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-C1"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-C1"));

    String productSku = productRepository.findById(productId).orElseThrow().getSku();
    var pending =
        replenishmentOrderService.findPending().stream()
            .filter(
                order ->
                    order.productSku().equals(productSku)
                        && order.warehouseName().equals("Main Warehouse"))
            .findFirst()
            .orElseThrow();
    int balanceBeforeCancellation = stockService.getBalance(productId, warehouseId).quantity();

    var cancelled = replenishmentOrderService.cancel(pending.id());

    assertThat(cancelled.status().name()).isEqualTo("CANCELLED");
    assertThat(cancelled.cancelledAt()).isNotNull();
    assertThat(cancelled.completedAt()).isNull();
    assertThat(stockService.getBalance(productId, warehouseId).quantity())
        .isEqualTo(balanceBeforeCancellation);

    assertThat(replenishmentOrderService.findPending())
        .noneMatch(order -> order.id().equals(pending.id()));
  }

  @Test
  void shouldRejectReceivingCancelledReplenishmentWithoutChangingStock() {
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-C2"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));
    registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-C2"));

    String productSku = productRepository.findById(productId).orElseThrow().getSku();
    var pending =
        replenishmentOrderService.findPending().stream()
            .filter(
                order ->
                    order.productSku().equals(productSku)
                        && order.warehouseName().equals("Main Warehouse"))
            .findFirst()
            .orElseThrow();

    replenishmentOrderService.cancel(pending.id());
    int balanceBeforeReceiveAttempt = stockService.getBalance(productId, warehouseId).quantity();

    assertThatThrownBy(() -> replenishmentOrderService.receive(pending.id()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Only pending replenishment orders can be received");

    assertThat(stockService.getBalance(productId, warehouseId).quantity())
        .isEqualTo(balanceBeforeReceiveAttempt);
  }

  @Test
  void shouldReplayStockMovementWithoutChangingBalanceTwice() {
    StockMovementRequest request =
        new StockMovementRequest(
            productId, warehouseId, MovementType.IN, 25, "purchase", "PO-IDEMP-1");

    StockMovementResponse first =
        stockService.registerMovement(request, "movement-idempotency-001");
    StockMovementResponse replay =
        stockService.registerMovement(request, "movement-idempotency-001");

    assertThat(replay).isEqualTo(first);
    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(25);

    var movements =
        stockService.findMovements(
            productId, warehouseId, null, null, null, PageRequest.of(0, 100));
    assertThat(movements.getContent())
        .filteredOn(movement -> "PO-IDEMP-1".equals(movement.reference()))
        .hasSize(1);
  }

  @Test
  void shouldReplayTransferWithoutMovingStockTwice() {
    WarehouseResponse destination =
        warehouseService.create(new CreateWarehouseRequest("Idempotent Destination", "RJ"));
    registerMovement(
        new StockMovementRequest(
            productId, warehouseId, MovementType.IN, 50, "purchase", "PO-IDEMP-2"));

    StockTransferRequest request =
        new StockTransferRequest(
            productId, warehouseId, destination.id(), 20, "Internal transfer", "TRF-IDEMP-1");

    StockTransferResponse first = stockService.transfer(request, "transfer-idempotency-001");
    StockTransferResponse replay = stockService.transfer(request, "transfer-idempotency-001");

    assertThat(replay).isEqualTo(first);
    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(30);
    assertThat(stockService.getBalance(productId, destination.id()).quantity()).isEqualTo(20);

    var movements =
        stockService.findMovements(productId, null, null, null, null, PageRequest.of(0, 100));
    assertThat(movements.getContent())
        .filteredOn(movement -> "TRF-IDEMP-1".equals(movement.reference()))
        .hasSize(2);
  }

  @Test
  void shouldRejectReusingIdempotencyKeyWithDifferentPayload() {
    StockMovementRequest firstRequest =
        new StockMovementRequest(
            productId, warehouseId, MovementType.IN, 10, "purchase", "PO-IDEMP-3");
    StockMovementRequest changedRequest =
        new StockMovementRequest(
            productId, warehouseId, MovementType.IN, 20, "purchase", "PO-IDEMP-3");

    stockService.registerMovement(firstRequest, "movement-idempotency-conflict");

    assertThatThrownBy(
            () ->
                stockService.registerMovement(
                    changedRequest, "movement-idempotency-conflict"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Idempotency-Key has already been used with a different request");

    assertThat(stockService.getBalance(productId, warehouseId).quantity()).isEqualTo(10);
  }

  private StockMovementResponse registerMovement(StockMovementRequest request) {
    return stockService.registerMovement(request, "test-movement-" + UUID.randomUUID());
  }

  private StockTransferResponse transfer(StockTransferRequest request) {
    return stockService.transfer(request, "test-transfer-" + UUID.randomUUID());
  }

}
