# StockFlow

Inventory management REST API — products, warehouses, stock movements, consolidated balances, and automatic reorder requests.

## Why this project

Built to demonstrate the parts of backend engineering that don't show up in a basic CRUD demo: schema evolution with versioned migrations, dynamic query composition, and integration tests that run against a real database instead of mocks.

## Stack

- Java 21 / Spring Boot 3.3
- PostgreSQL 16
- Flyway (versioned schema migrations — Hibernate `ddl-auto` is set to `validate`, not `update`; the schema is owned by SQL migrations, the way it should be in a real system)
- Spring Data JPA + JPA Specifications (dynamic, composable filtering)
- Testcontainers (integration tests run against a real Postgres container, migrated by Flyway, not H2 or mocks)
- SpringDoc OpenAPI (Swagger UI)

## Domain & business rules

- **Stock** is a consolidated balance per product/warehouse pair, protected by optimistic locking (`@Version`) to catch concurrent-update conflicts.
- **StockMovement** is an immutable audit trail — every IN/OUT is recorded and never edited.
- An `OUT` movement that would take the balance negative is rejected — balance can never go below zero.
- When a movement drops a product's balance below its configured minimum stock, a **ReplenishmentOrder** is created automatically — but only if one isn't already pending for that product/warehouse, to avoid duplicate reorder requests.
- Movement history can be filtered dynamically (product, warehouse, type, date range) via **JPA Specifications**, composed from small reusable predicates instead of one bloated repository method per filter combination.

## Running locally

```bash
docker-compose up -d          # starts PostgreSQL on localhost:5432
mvn flyway:migrate            # optional - Flyway also runs automatically on app startup
mvn spring-boot:run           # starts the API on http://localhost:8080
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Running the tests

```bash
mvn test
```

The integration test (`StockMovementIntegrationTest`) spins up a real PostgreSQL container via Testcontainers, runs the actual Flyway migrations against it, and exercises the full Spring context — not mocked repositories. Requires Docker running locally.

## API overview

| Resource | Endpoints |
|---|---|
| Products | `POST/GET /api/products`, `GET/PUT /api/products/{id}` |
| Warehouses | `POST/GET /api/warehouses` |
| Stock movements | `POST /api/stock/movements`, `GET /api/stock/movements` (filterable + paginated) |
| Stock balance | `GET /api/stock/balance?productId=&warehouseId=`, `GET /api/stock/balance/product/{productId}` |
| Replenishment orders | `GET /api/replenishment-orders/pending`, `POST /api/replenishment-orders/{id}/complete` |

## Status

Actively maintained portfolio project.
