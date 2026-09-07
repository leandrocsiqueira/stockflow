# StockFlow

[![CI](https://github.com/leandrocsiqueira/stockflow/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/leandrocsiqueira/stockflow/actions/workflows/ci.yml)

Inventory management REST API for products, warehouses, stock movements, stock balances, and automatic replenishment orders.

## About

StockFlow is a backend portfolio project focused on inventory management and business rules beyond basic CRUD operations.

The application supports product and warehouse management, stock IN/OUT movements, consolidated balances, automatic replenishment orders, dynamic filtering, pagination, standardized API errors, and OpenAPI documentation.

## Stack

| Technology | Version |
|---|---|
| Java | 25 |
| Spring Boot | 3.5.16 |
| PostgreSQL | 16 |
| Testcontainers | 1.20.1 |
| SpringDoc OpenAPI | 2.8.17 |

The project also uses Spring Web, Spring Data JPA, Hibernate, Bean Validation, Flyway, JPA Specifications, JUnit 5, Mockito, and MockMvc.

Database schema evolution is managed by Flyway. Hibernate `ddl-auto` is configured as `validate`.

## Business rules

Stock is maintained per product and warehouse, with optimistic locking through `@Version`.

Every IN or OUT operation creates an immutable stock movement. OUT movements cannot produce negative stock.

When the balance falls below the configured minimum stock, a replenishment order is automatically created if another pending order does not already exist for the same product and warehouse.

Movement history supports dynamic filtering by product, warehouse, movement type, and date range using JPA Specifications.

## API

| Resource | Endpoints |
|---|---|
| Products | `POST/GET /api/products`, `GET/PUT /api/products/{id}` |
| Warehouses | `POST/GET /api/warehouses` |
| Stock movements | `POST /api/stock/movements`, `GET /api/stock/movements` |
| Stock balance | `GET /api/stock/balance`, `GET /api/stock/balance/product/{productId}` |
| Replenishment orders | `GET /api/replenishment-orders/pending`, `POST /api/replenishment-orders/{id}/complete` |

Stock movement history supports filtering, sorting, and pagination.

The API also provides standardized responses for validation errors, malformed JSON, missing resources, and business rule conflicts.

## Web client

A React and TypeScript frontend for this API is available in the StockFlow Web repository:

https://github.com/leandrocsiqueira/stockflow-web

## Running locally

```bash
docker-compose up -d
mvn spring-boot:run
```

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI specification:

```text
http://localhost:8080/v3/api-docs
```

## Tests

Docker must be running for the Testcontainers integration tests.

```bash
mvn clean test
```

The test suite includes unit tests, MockMvc controller tests, repository tests, and integration tests against PostgreSQL with Flyway migrations.

## Status

Version 1.0.0 - portfolio release.
