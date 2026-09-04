package com.leandro.stockflow.service;

import com.leandro.stockflow.dto.CreateProductRequest;
import com.leandro.stockflow.dto.UpdateProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.entity.Product;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.exception.ResourceNotFoundException;
import com.leandro.stockflow.mapper.ProductMapper;
import com.leandro.stockflow.repository.ProductRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

  private final ProductRepository productRepository;

  public ProductService(ProductRepository productRepository) {
    this.productRepository = productRepository;
  }

  @Transactional
  public ProductResponse create(CreateProductRequest request) {
    productRepository
        .findBySku(request.sku())
        .ifPresent(
            p -> {
              throw new BusinessRuleException(
                  "A product with SKU " + request.sku() + " already exists");
            });
    Product product =
        new Product(request.sku(), request.name(), request.unit(), request.minimumStock());
    return ProductMapper.toResponse(productRepository.save(product));
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> findAll() {
    return productRepository.findAll().stream().map(ProductMapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public ProductResponse findById(Long id) {
    return ProductMapper.toResponse(getOrThrow(id));
  }

  @Transactional
  public ProductResponse update(Long id, UpdateProductRequest request) {
    Product product = getOrThrow(id);
    product.setName(request.name());
    product.setMinimumStock(request.minimumStock());
    return ProductMapper.toResponse(product);
  }

  protected Product getOrThrow(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
  }
}
