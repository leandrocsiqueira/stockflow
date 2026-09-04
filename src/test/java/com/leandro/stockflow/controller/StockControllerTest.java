package com.leandro.stockflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
import com.leandro.stockflow.dto.StockPolicyRequest;
import com.leandro.stockflow.dto.StockResponse;
import com.leandro.stockflow.dto.StockTransferRequest;
import com.leandro.stockflow.dto.StockTransferResponse;
import com.leandro.stockflow.entity.MovementType;
import com.leandro.stockflow.service.StockService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StockController.class)
class StockControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StockService stockService;

  @Test
  void shouldRegisterStockMovement() throws Exception {
    StockMovementResponse response =
        new StockMovementResponse(
            1L,
            "SKU-001",
            "Keyboard",
            "Main Warehouse",
            MovementType.IN,
            10,
            "Initial stock",
            "REF-001",
            LocalDateTime.of(2026, 8, 31, 10, 0));

    when(stockService.registerMovement(any(StockMovementRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/stock/movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": 1,
                      "warehouseId": 1,
                      "type": "IN",
                      "quantity": 10,
                      "reason": "Initial stock",
                      "reference": "REF-001"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.productSku").value("SKU-001"))
        .andExpect(jsonPath("$.warehouseName").value("Main Warehouse"))
        .andExpect(jsonPath("$.type").value("IN"))
        .andExpect(jsonPath("$.quantity").value(10));
  }

  @Test
  void shouldTransferStockBetweenWarehouses() throws Exception {
    StockTransferResponse response =
        new StockTransferResponse(
            10L,
            11L,
            "SKU-001",
            "Keyboard",
            "Main Warehouse",
            "Secondary Warehouse",
            20,
            "Internal transfer",
            "TRF-001",
            30,
            20,
            LocalDateTime.of(2026, 9, 4, 10, 0));

    when(stockService.transfer(any(StockTransferRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/stock/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": 1,
                      "sourceWarehouseId": 1,
                      "destinationWarehouseId": 2,
                      "quantity": 20,
                      "reason": "Internal transfer",
                      "reference": "TRF-001"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sourceMovementId").value(10))
        .andExpect(jsonPath("$.destinationMovementId").value(11))
        .andExpect(jsonPath("$.productSku").value("SKU-001"))
        .andExpect(jsonPath("$.sourceWarehouseName").value("Main Warehouse"))
        .andExpect(jsonPath("$.destinationWarehouseName").value("Secondary Warehouse"))
        .andExpect(jsonPath("$.quantity").value(20))
        .andExpect(jsonPath("$.sourceBalance").value(30))
        .andExpect(jsonPath("$.destinationBalance").value(20))
        .andExpect(jsonPath("$.reference").value("TRF-001"));
  }

  @Test
  void shouldRejectTransferWithoutReference() throws Exception {
    mockMvc
        .perform(
            post("/api/stock/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": 1,
                      "sourceWarehouseId": 1,
                      "destinationWarehouseId": 2,
                      "quantity": 20
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.details[0].field").value("reference"));
  }

  @Test
  void shouldConfigureInventoryPolicy() throws Exception {
    StockResponse response =
        new StockResponse(1L, "SKU-001", "Keyboard", "Main Warehouse", 15, 10, 30, false);

    when(stockService.configurePolicy(any(StockPolicyRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/stock/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": 1,
                      "warehouseId": 1,
                      "reorderPoint": 10,
                      "targetStock": 30
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reorderPoint").value(10))
        .andExpect(jsonPath("$.targetStock").value(30))
        .andExpect(jsonPath("$.belowReorderPoint").value(false));
  }

  @Test
  void shouldRejectInventoryPolicyWhenTargetIsBelowReorderPoint() throws Exception {
    mockMvc
        .perform(
            put("/api/stock/policies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "productId": 1,
                      "warehouseId": 1,
                      "reorderPoint": 10,
                      "targetStock": 9
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"));
  }

  @Test
  void shouldReturnPagedStockMovements() throws Exception {
    StockMovementResponse movement =
        new StockMovementResponse(
            1L,
            "SKU-001",
            "Keyboard",
            "Main Warehouse",
            MovementType.IN,
            10,
            "Initial stock",
            "REF-001",
            LocalDateTime.of(2026, 8, 31, 10, 0));

    when(stockService.findMovements(
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(movement)));

    mockMvc
        .perform(get("/api/stock/movements?page=0&size=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(1))
        .andExpect(jsonPath("$.content[0].productSku").value("SKU-001"))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(true));
  }
}
