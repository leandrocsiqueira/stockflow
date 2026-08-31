package com.leandro.stockflow.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiError> handleNotFound(
      ResourceNotFoundException ex, HttpServletRequest request) {
    return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
  }

  @ExceptionHandler(BusinessRuleException.class)
  public ResponseEntity<ApiError> handleBusinessRule(
      BusinessRuleException ex, HttpServletRequest request) {
    return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<ApiFieldError> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new ApiFieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();
    return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleMalformedJson(
      HttpMessageNotReadableException ex, HttpServletRequest request) {

    return build(
        HttpStatus.BAD_REQUEST, "Malformed JSON request", request.getRequestURI(), List.of());
  }

  private ResponseEntity<ApiError> build(
      HttpStatus status, String message, String path, List<ApiFieldError> details) {

    ApiError error =
        new ApiError(
            Instant.now(), status.value(), status.getReasonPhrase(), message, path, details);
    return ResponseEntity.status(status).body(error);
  }
}
