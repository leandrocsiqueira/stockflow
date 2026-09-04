package com.leandro.stockflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.leandro.stockflow.dto.CreateProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.dto.UpdateProductRequest;
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

    when(productService.create(any(CreateProductRequest.class))).thenReturn(response);

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
  void shouldUpdateProductMutableFields() throws Exception {
    ProductResponse response = new ProductResponse(1L, "SKU-001", "Mechanical Keyboard", "UN", 10);

    when(productService.update(eq(1L), any(UpdateProductRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Mechanical Keyboard",
                      "minimumStock": 10
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.sku").value("SKU-001"))
        .andExpect(jsonPath("$.unit").value("UN"))
        .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
        .andExpect(jsonPath("$.minimumStock").value(10));
  }

  @Test
  void shouldReturnBadRequestWhenUpdateIsInvalid() throws Exception {
    mockMvc
        .perform(
            put("/api/products/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "",
                      "minimumStock": -1
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.path").value("/api/products/1"))
        .andExpect(jsonPath("$.details").isArray());
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
