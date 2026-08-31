package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
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
    description = "Auto-generated reorder requests when stock falls below minimum")
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

  @Operation(summary = "Complete a replenishment order")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Replenishment order completed successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Replenishment order not found",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Replenishment order cannot be completed",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  @PostMapping("/{id}/complete")
  public ResponseEntity<ReplenishmentOrderResponse> complete(@PathVariable Long id) {
    return ResponseEntity.ok(replenishmentOrderService.complete(id));
  }
}
