package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.dto.StockResponse;
import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.service.StockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock", description = "Stock movements, balances, and movement history")
public class StockController {

  private final StockService stockService;

  public StockController(StockService stockService) {
    this.stockService = stockService;
  }

  @PostMapping("/movements")
  public ResponseEntity<StockMovementResponse> registerMovement(
      @Valid @RequestBody StockMovementRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stockService.registerMovement(request));
  }

  @GetMapping("/movements")
  public ResponseEntity<Page<StockMovementResponse>> findMovements(
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long warehouseId,
      @RequestParam(required = false) MovementType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        stockService.findMovements(productId, warehouseId, type, from, to, pageable));
  }

  @GetMapping("/balance")
  public ResponseEntity<StockResponse> getBalance(
      @RequestParam Long productId, @RequestParam Long warehouseId) {
    return ResponseEntity.ok(stockService.getBalance(productId, warehouseId));
  }

  @GetMapping("/balance/product/{productId}")
  public ResponseEntity<List<StockResponse>> getBalancesForProduct(@PathVariable Long productId) {
    return ResponseEntity.ok(stockService.getBalancesForProduct(productId));
  }
}
