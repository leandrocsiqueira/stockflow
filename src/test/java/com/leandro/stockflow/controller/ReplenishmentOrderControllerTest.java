package com.leandro.stockflow.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leandro.stockflow.dto.ReplenishmentOrderResponse;
import com.leandro.stockflow.dto.ReplenishmentReceiptResponse;
import com.leandro.stockflow.entity.ReplenishmentStatus;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.service.ReplenishmentOrderService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReplenishmentOrderController.class)
class ReplenishmentOrderControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ReplenishmentOrderService replenishmentOrderService;

  @Test
  void shouldReceiveReplenishmentOrder() throws Exception {
    LocalDateTime receivedAt = LocalDateTime.of(2026, 9, 4, 10, 0);
    ReplenishmentReceiptResponse response =
        new ReplenishmentReceiptResponse(
            7L,
            20L,
            "SKU-001",
            "Keyboard",
            "Main Warehouse",
            25,
            30,
            "REPLENISHMENT-7",
            receivedAt);

    when(replenishmentOrderService.receive(7L)).thenReturn(response);

    mockMvc
        .perform(post("/api/replenishment-orders/7/receive"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderId").value(7))
        .andExpect(jsonPath("$.movementId").value(20))
        .andExpect(jsonPath("$.receivedQuantity").value(25))
        .andExpect(jsonPath("$.resultingBalance").value(30))
        .andExpect(jsonPath("$.reference").value("REPLENISHMENT-7"));
  }

  @Test
  void shouldCancelReplenishmentOrder() throws Exception {
    LocalDateTime createdAt = LocalDateTime.of(2026, 9, 4, 9, 0);
    LocalDateTime cancelledAt = LocalDateTime.of(2026, 9, 4, 10, 0);
    ReplenishmentOrderResponse response =
        new ReplenishmentOrderResponse(
            7L,
            "SKU-001",
            "Keyboard",
            "Main Warehouse",
            25,
            ReplenishmentStatus.CANCELLED,
            createdAt,
            null,
            cancelledAt);

    when(replenishmentOrderService.cancel(7L)).thenReturn(response);

    mockMvc
        .perform(post("/api/replenishment-orders/7/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.cancelledAt").exists());
  }

  @Test
  void shouldReturnConflictWhenReceivingNonPendingOrder() throws Exception {
    when(replenishmentOrderService.receive(7L))
        .thenThrow(new BusinessRuleException("Only pending replenishment orders can be received"));

    mockMvc
        .perform(post("/api/replenishment-orders/7/receive"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(
            jsonPath("$.message").value("Only pending replenishment orders can be received"));
  }
}
