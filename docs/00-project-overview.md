# StockFlow - Project Overview

## Purpose

StockFlow is an inventory management REST API built as a backend portfolio project. Its main purpose is to model inventory operations that go beyond basic CRUD by enforcing stock consistency, recording movement history and automating replenishment decisions.

The backend is implemented with Java 25 and Spring Boot 3.5.16. PostgreSQL is used as the persistent database, Flyway manages schema evolution and Testcontainers is used for integration tests against a real PostgreSQL instance.

## Current domain

| Concept | Responsibility |
|---|---|
| Product | Represents an inventory item identified by SKU, name and unit |
| Warehouse | Represents a physical or logical storage location |
| Stock | Stores the current quantity for one product in one warehouse |
| StockMovement | Records an immutable IN or OUT operation |
| ReplenishmentOrder | Represents an automatically generated replenishment request |

## Main business flow

A stock movement is submitted through the HTTP API. The application loads the product and warehouse, obtains or creates the corresponding stock record, changes the quantity, persists an immutable movement record and evaluates whether replenishment is required.

```text
HTTP request
    |
    v
StockController
    |
    v
StockService.registerMovement()
    |
    +--> ProductRepository
    |
    +--> WarehouseRepository
    |
    +--> StockRepository
    |       |
    |       +--> Stock.increase() or Stock.decrease()
    |
    +--> StockMovementRepository
    |
    +--> minimum stock evaluation
            |
            +--> ReplenishmentOrderRepository
```

## Business rules already implemented in V1

| Rule | Current behavior |
|---|---|
| Product SKU | Must be unique |
| Product minimum stock | Cannot be negative |
| Stock quantity | Cannot become negative |
| Stock movement quantity | Must be positive |
| Movement history | Every IN or OUT operation creates a record |
| Stock scope | Quantity is maintained per product and warehouse |
| Replenishment | Automatically created when stock falls below product minimum |
| Duplicate replenishment | Only one pending order is allowed per product and warehouse |
| Movement search | Supports product, warehouse, type and date filters |
| Pagination | Available for stock movement history |
| API errors | Validation, business rules and missing resources use standardized responses |

## Current technical stack

| Technology | Purpose |
|---|---|
| Java 25 | Programming language |
| Spring Boot 3.5.16 | Application framework |
| Spring Web | REST API |
| Spring Data JPA | Persistence abstraction |
| Hibernate | ORM implementation |
| PostgreSQL | Relational database |
| Flyway | Database migrations |
| Bean Validation | HTTP request validation |
| SpringDoc OpenAPI | Swagger UI and OpenAPI specification |
| JUnit 5 | Automated tests |
| Mockito and MockMvc | Unit and controller tests |
| Testcontainers | PostgreSQL integration tests |

## Version strategy

Version 1.0.0 is the first portfolio release and should remain frozen as a historical reference.

Version 2.0.0 is the active evolution line. During development the Maven version is `2.0.0-SNAPSHOT`.

The V2 objective is not to expand the project indefinitely. It is to improve the domain model, API consistency, maintainability, observability and test coverage while adding a limited set of meaningful inventory operations.
