package com.leandro.stockflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leandro.stockflow.entity.InventoryOperation;
import com.leandro.stockflow.entity.InventoryOperationType;
import com.leandro.stockflow.exception.BusinessRuleException;
import com.leandro.stockflow.repository.InventoryOperationRepository;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

  private static final int MAX_KEY_LENGTH = 100;

  private final InventoryOperationRepository operationRepository;
  private final ObjectMapper objectMapper;

  public IdempotencyService(
      InventoryOperationRepository operationRepository, ObjectMapper objectMapper) {
    this.operationRepository = operationRepository;
    this.objectMapper = objectMapper;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public <T> T execute(
      String idempotencyKey,
      InventoryOperationType operationType,
      String requestHash,
      Class<T> responseType,
      Supplier<T> operation) {
    validateKey(idempotencyKey);

    int reserved =
        operationRepository.reserve(idempotencyKey, operationType.name(), requestHash);
    InventoryOperation record =
        operationRepository
            .findByIdempotencyKey(idempotencyKey)
            .orElseThrow(() -> new IllegalStateException("Idempotency reservation was not found"));

    if (reserved == 0) {
      validateReplay(record, operationType, requestHash);
      return deserialize(record.getResponseBody(), responseType);
    }

    T response = operation.get();
    record.complete(serialize(response));
    return response;
  }

  private void validateKey(String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw new BusinessRuleException("Idempotency-Key must not be blank");
    }
    if (idempotencyKey.length() > MAX_KEY_LENGTH) {
      throw new BusinessRuleException("Idempotency-Key must not exceed 100 characters");
    }
  }

  private void validateReplay(
      InventoryOperation record, InventoryOperationType operationType, String requestHash) {
    if (record.getOperationType() != operationType || !record.getRequestHash().equals(requestHash)) {
      throw new BusinessRuleException(
          "Idempotency-Key has already been used with a different request");
    }
    if (record.getResponseBody() == null) {
      throw new IllegalStateException("Completed idempotency record has no response body");
    }
  }

  private String serialize(Object response) {
    try {
      return objectMapper.writeValueAsString(response);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Could not persist idempotent response", ex);
    }
  }

  private <T> T deserialize(String responseBody, Class<T> responseType) {
    try {
      return objectMapper.readValue(responseBody, responseType);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Could not replay idempotent response", ex);
    }
  }
}
