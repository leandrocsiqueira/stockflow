package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
import com.leandro.stockflow.dto.ReplenishmentReceiptResponse;
import com.leandro.stockflow.exception.ApiError;
import com.leandro.stockflow.service.ReplenishmentOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/replenishment-orders")
@Tag(
    name = "Replenishment Orders",
    description = "Automatic replenishment lifecycle for low-stock inventory")
public class ReplenishmentOrderController {

  private final ReplenishmentOrderService replenishmentOrderService;

  public ReplenishmentOrderController(ReplenishmentOrderService replenishmentOrderService) {
    this.replenishmentOrderService = replenishmentOrderService;
  }

  @Operation(summary = "List pending replenishment orders")
  @ApiResponse(
      responseCode = "200",
      description = "Pending replenishment orders retrieved successfully")
  @GetMapping("/pending")
  public ResponseEntity<List<ReplenishmentOrderResponse>> findPending() {
    return ResponseEntity.ok(replenishmentOrderService.findPending());
  }

  @Operation(
      summary = "Receive a replenishment order",
      description =
          "Adds the requested quantity to inventory, records an IN movement and completes the order atomically")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Replenishment received successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Replenishment order not found",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Replenishment order cannot be received",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping("/{id}/receive")
  public ResponseEntity<ReplenishmentReceiptResponse> receive(@PathVariable Long id) {
    return ResponseEntity.ok(replenishmentOrderService.receive(id));
  }

  @Operation(summary = "Cancel a pending replenishment order")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Replenishment order cancelled successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Replenishment order not found",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Replenishment order cannot be cancelled",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping("/{id}/cancel")
  public ResponseEntity<ReplenishmentOrderResponse> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(replenishmentOrderService.cancel(id));
  }
}
