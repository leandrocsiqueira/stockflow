package com.leandro.stockflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leandro.stockflow.dto.CreateWarehouseRequest;
import com.leandro.stockflow.dto.UpdateWarehouseRequest;
import com.leandro.stockflow.dto.WarehouseResponse;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.service.WarehouseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WarehouseController.class)
class WarehouseControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private WarehouseService warehouseService;

  @Test
  void shouldCreateWarehouse() throws Exception {
    WarehouseResponse response = new WarehouseResponse(1L, "Main Warehouse", "SP");
    when(warehouseService.create(any(CreateWarehouseRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Main Warehouse",
                      "location": "SP"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Main Warehouse"))
        .andExpect(jsonPath("$.location").value("SP"));
  }

  @Test
  void shouldReturnBadRequestWhenWarehouseIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/warehouses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "",
                      "location": "SP"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.path").value("/api/warehouses"));
  }

  @Test
  void shouldReturnWarehouseById() throws Exception {
    WarehouseResponse response = new WarehouseResponse(1L, "Main Warehouse", "SP");
    when(warehouseService.findById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/warehouses/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Main Warehouse"));
  }

  @Test
  void shouldReturnNotFoundWhenWarehouseDoesNotExist() throws Exception {
    when(warehouseService.findById(999L))
        .thenThrow(new ResourceNotFoundException("Warehouse not found with id: 999"));

    mockMvc
        .perform(get("/api/warehouses/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("Warehouse not found with id: 999"))
        .andExpect(jsonPath("$.path").value("/api/warehouses/999"));
  }

  @Test
  void shouldUpdateWarehouse() throws Exception {
    WarehouseResponse response = new WarehouseResponse(1L, "Distribution Center", "MG");
    when(warehouseService.update(eq(1L), any(UpdateWarehouseRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/warehouses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Distribution Center",
                      "location": "MG"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("Distribution Center"))
        .andExpect(jsonPath("$.location").value("MG"));
  }

  @Test
  void shouldReturnBadRequestWhenWarehouseUpdateIsInvalid() throws Exception {
    mockMvc
        .perform(
            put("/api/warehouses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "",
                      "location": "MG"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.path").value("/api/warehouses/1"));
  }
}
