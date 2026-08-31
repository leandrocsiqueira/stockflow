package com.leandro.stockflow.mapper;

import com.leandro.stockflow.dto.StockResponse;
import com.leandro.stockflow.entity.Stock;

public class StockMapper {

  private StockMapper() {}

  public static StockResponse toResponse(Stock stock) {
    return new StockResponse(
        stock.getId(),
        stock.getProduct().getSku(),
        stock.getProduct().getName(),
        stock.getWarehouse().getName(),
        stock.getQuantity(),
        stock.getProduct().getMinimumStock(),
        stock.isBelowMinimum());
  }
}
