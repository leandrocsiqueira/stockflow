package com.leandro.stockflow.exception;

public class BusinessRuleException extends RuntimeException {
  public BusinessRuleException(String message) {
    super(message);
  }
}
