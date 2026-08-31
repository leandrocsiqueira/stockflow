package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.PageResponse;
import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.dto.StockResponse;
import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.exception.ApiError;
import com.leandro.stockflow.service.StockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
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
  @Operation(summary = "Register a stock movement")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Stock movement registered successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Product or warehouse not found",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Stock movement violates a business rule",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<StockMovementResponse> registerMovement(
      @Valid @RequestBody StockMovementRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stockService.registerMovement(request));
  }

  @GetMapping("/movements")
  @Operation(summary = "Search stock movement history")
  @ApiResponse(responseCode = "200", description = "Stock movements retrieved successfully")
  public ResponseEntity<PageResponse<StockMovementResponse>> findMovements(
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long warehouseId,
      @RequestParam(required = false) MovementType type,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @ParameterObject
          @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(
        PageResponse.from(
            stockService.findMovements(productId, warehouseId, type, from, to, pageable)));
  }

  @GetMapping("/balance")
  @Operation(summary = "Get stock balance")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Stock balance retrieved successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Stock record not found",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<StockResponse> getBalance(
      @RequestParam Long productId, @RequestParam Long warehouseId) {
    return ResponseEntity.ok(stockService.getBalance(productId, warehouseId));
  }

  @GetMapping("/balance/product/{productId}")
  @Operation(summary = "List stock balances for a product")
  @ApiResponse(responseCode = "200", description = "Stock balances retrieved successfully")
  public ResponseEntity<List<StockResponse>> getBalancesForProduct(@PathVariable Long productId) {
    return ResponseEntity.ok(stockService.getBalancesForProduct(productId));
  }
}
