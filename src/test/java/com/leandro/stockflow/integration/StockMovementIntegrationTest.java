package com.leandro.stockflow.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.leandro.stockflow.dto.CreateProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.dto.StockPolicyRequest;
import com.leandro.stockflow.dto.StockResponse;
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
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 50, "purchase", "PO-1"));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 30, "purchase", "PO-2"));
    StockResponse balance = stockService.getBalance(productId, warehouseId);
    assertThat(balance.quantity()).isEqualTo(80);
  }

  @Test
  void shouldDecreaseStockOnOutMovementAndKeepBalanceConsistent() {
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 100, "purchase", "PO-1"));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 40, "sale", "SO-1"));
    StockResponse balance = stockService.getBalance(productId, warehouseId);
    assertThat(balance.quantity()).isEqualTo(60);
  }

  @Test
  void shouldRejectOutMovementExceedingAvailableBalance() {
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-1"));
    assertThatThrownBy(
            () ->
                stockService.registerMovement(
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
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-1"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));

    stockService.registerMovement(
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
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-1"));
    stockService.configurePolicy(new StockPolicyRequest(productId, warehouseId, 10, 30));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-1"));

    long firstCount = replenishmentOrderService.findPending().size();

    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 1, "purchase", "PO-2"));

    long secondCount = replenishmentOrderService.findPending().size();
    assertThat(secondCount).isEqualTo(firstCount);
  }

  @Test
  void shouldKeepInventoryPolicySpecificToEachWarehouse() {
    WarehouseResponse secondaryWarehouse =
        warehouseService.create(new CreateWarehouseRequest("Secondary Warehouse", "MG"));

    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 15, "purchase", "PO-1"));
    stockService.registerMovement(
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
  void shouldFilterMovementHistoryByTypeUsingSpecification() {
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 50, "purchase", "PO-1"));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 10, "sale", "SO-1"));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 5, "sale", "SO-2"));
    var outMovements =
        stockService.findMovements(
            productId, warehouseId, MovementType.OUT, null, null, PageRequest.of(0, 10));
    assertThat(outMovements.getContent()).hasSize(2);
    assertThat(outMovements.getContent()).allMatch(m -> m.type() == MovementType.OUT);
  }

  @Test
  void shouldKeepBalanceConsistentWithMovementHistory() {
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 100, "purchase", "PO-1"));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.OUT, 30, "sale", "SO-1"));
    stockService.registerMovement(
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
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 100, "purchase", "PO-1"));
    stockService.registerMovement(
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
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-1"));
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 20, "purchase", "PO-2"));
    stockService.registerMovement(
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
    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 10, "purchase", "PO-1"));

    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 30, "purchase", "PO-2"));

    stockService.registerMovement(
        new StockMovementRequest(productId, warehouseId, MovementType.IN, 20, "purchase", "PO-3"));

    var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "quantity"));

    var page = stockService.findMovements(productId, warehouseId, null, null, null, pageable);

    assertThat(page.getContent())
        .extracting(StockMovementResponse::quantity)
        .containsExactly(30, 20, 10);
  }
}
