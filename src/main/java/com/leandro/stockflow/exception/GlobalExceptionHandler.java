package com.leandro.stockflow.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of());
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", details);
  }

  private ResponseEntity<ApiError> build(HttpStatus status, String message, List<String> details) {
    ApiError error =
        new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details);
    return ResponseEntity.status(status).body(error);
  }
}
