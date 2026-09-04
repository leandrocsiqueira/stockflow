package com.leandro.stockflow.service;

import com.leandro.stockflow.dto.StockMovementRequest;
import com.leandro.stockflow.dto.StockTransferRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class RequestFingerprint {

  private RequestFingerprint() {}

  static String forMovement(StockMovementRequest request) {
    return sha256(
        canonical(
            request.productId(),
            request.warehouseId(),
            request.type(),
            request.quantity(),
            request.reason(),
            request.reference()));
  }

  static String forTransfer(StockTransferRequest request) {
    return sha256(
        canonical(
            request.productId(),
            request.sourceWarehouseId(),
            request.destinationWarehouseId(),
            request.quantity(),
            request.reason(),
            request.reference()));
  }

  private static String canonical(Object... values) {
    StringBuilder result = new StringBuilder();
    for (Object value : values) {
      String text = value == null ? "<null>" : value.toString();
      result.append(text.length()).append(':').append(text).append('|');
    }
    return result.toString();
  }

  private static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 is not available", ex);
    }
  }
}
