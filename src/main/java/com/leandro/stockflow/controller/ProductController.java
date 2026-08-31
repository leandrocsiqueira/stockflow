package com.leandro.stockflow.controller;

import com.leandro.stockflow.dto.ProductRequest;
import com.leandro.stockflow.dto.ProductResponse;
import com.leandro.stockflow.exception.ApiError;
import com.leandro.stockflow.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/products")
@RestController
@Tag(
    name = "Products",
    description = "Product registration, listing, retrieval, and update operations")
public class ProductController {
  private final ProductService productService;

  public ProductController(ProductService productService) {
    this.productService = productService;
  }

  @PostMapping
  @Operation(
      summary = "Create a product",
      description = "Creates a new product in the inventory catalog")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Product created successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "409",
        description = "Product SKU already exists",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
  }

  @GetMapping
  @Operation(summary = "List products", description = "Returns all registered products")
  @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
  public ResponseEntity<List<ProductResponse>> findAll() {
    return ResponseEntity.ok(productService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get product by ID", description = "Returns a product identified by its ID")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Product retrieved successfully"),
    @ApiResponse(
        responseCode = "404",
        description = "Product not found",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<ProductResponse> findById(@PathVariable Long id) {
    return ResponseEntity.ok(productService.findById(id));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update a product", description = "Updates an existing product")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Product updated successfully"),
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(
        responseCode = "404",
        description = "Product not found",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
  })
  public ResponseEntity<ProductResponse> update(
      @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return ResponseEntity.ok(productService.update(id, request));
  }
}
