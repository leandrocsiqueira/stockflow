package com.leandro.stockflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leandro.stockflow.dto.ProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private ProductService productService;

  @Test
  void shouldCreateProduct() throws Exception {
    ProductResponse response = new ProductResponse(1L, "SKU-001", "Keyboard", "UN", 5);

    when(productService.create(any(ProductRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sku": "SKU-001",
                      "name": "Keyboard",
                      "unit": "UN",
                      "minimumStock": 5
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.sku").value("SKU-001"))
        .andExpect(jsonPath("$.name").value("Keyboard"));
  }

  @Test
  void shouldReturnProductById() throws Exception {
    ProductResponse response = new ProductResponse(1L, "SKU-001", "Keyboard", "UN", 5);

    when(productService.findById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/products/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.sku").value("SKU-001"));
  }

  @Test
  void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
    when(productService.findById(999L))
        .thenThrow(new ResourceNotFoundException("Product not found with id: 999"));

    mockMvc
        .perform(get("/api/products/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Product not found with id: 999"))
        .andExpect(jsonPath("$.path").value("/api/products/999"));
  }

  @Test
  void shouldReturnBadRequestWhenProductIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sku": "",
                      "name": "",
                      "unit": "",
                      "minimumStock": -1
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.path").value("/api/products"))
        .andExpect(jsonPath("$.details").isArray());
  }

  @Test
  void shouldReturnBadRequestWhenJsonIsMalformed() throws Exception {
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "sku": "SKU-001",
                      "name": "Keyboard",
                      "unit": "UN",
                      "minimumStock": {}
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Malformed JSON request"))
        .andExpect(jsonPath("$.path").value("/api/products"));
  }
}
