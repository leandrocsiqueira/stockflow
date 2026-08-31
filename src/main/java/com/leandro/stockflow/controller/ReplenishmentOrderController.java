package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
import com.leandro.stockflow.service.ReplenishmentOrderService;
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

  @GetMapping("/pending")
  public ResponseEntity<List<ReplenishmentOrderResponse>> findPending() {
    return ResponseEntity.ok(replenishmentOrderService.findPending());
  }

  @PostMapping("/{id}/complete")
  public ResponseEntity<ReplenishmentOrderResponse> complete(@PathVariable Long id) {
    return ResponseEntity.ok(replenishmentOrderService.complete(id));
  }
}
