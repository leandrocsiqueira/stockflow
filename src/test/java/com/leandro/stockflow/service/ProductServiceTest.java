package com.leandro.stockflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leandro.stockflow.dto.CreateProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.dto.UpdateProductRequest;
import com.leandro.stockflow.entity.Product;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.repository.ProductRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  @InjectMocks private ProductService productService;

  @Test
  void shouldCreateProduct() {
    CreateProductRequest request = new CreateProductRequest("SKU-001", "Keyboard", "UN");

    when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
    when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ProductResponse response = productService.create(request);

    assertThat(response.sku()).isEqualTo("SKU-001");
    assertThat(response.name()).isEqualTo("Keyboard");
    assertThat(response.unit()).isEqualTo("UN");
    verify(productRepository).save(any(Product.class));
  }

  @Test
  void shouldRejectDuplicateSku() {
    CreateProductRequest request = new CreateProductRequest("SKU-001", "Keyboard", "UN");
    Product existing = new Product("SKU-001", "Existing product", "UN");

    when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> productService.create(request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A product with SKU SKU-001 already exists");
  }

  @Test
  void shouldUpdateOnlyMutableFields() {
    Product product = new Product("SKU-001", "Keyboard", "UN");
    UpdateProductRequest request = new UpdateProductRequest("Mechanical Keyboard");

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    ProductResponse response = productService.update(1L, request);

    assertThat(response.sku()).isEqualTo("SKU-001");
    assertThat(response.unit()).isEqualTo("UN");
    assertThat(response.name()).isEqualTo("Mechanical Keyboard");
  }

  @Test
  void shouldThrowWhenUpdatingMissingProduct() {
    when(productRepository.findById(999L)).thenReturn(Optional.empty());

    UpdateProductRequest request = new UpdateProductRequest("Keyboard");

    assertThatThrownBy(() -> productService.update(999L, request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Product not found with id: 999");
  }
}
