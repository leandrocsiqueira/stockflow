package com.leandro.stockflow.mapper;

import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.entity.Product;

public class ProductMapper {

  private ProductMapper() {}

  public static ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(), product.getSku(), product.getName(), product.getUnit());
  }
}
