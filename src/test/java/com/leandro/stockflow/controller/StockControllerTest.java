package com.leandro.stockflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockMovementResponse;
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
