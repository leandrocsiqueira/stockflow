# StockFlow - Architecture

## V1 package organization

The current codebase follows a technical layered architecture.

```text
com.leandro.stockflow
    config
    controller
    dto
    entity
    exception
    mapper
    repository
    service
    specification
```

Each layer has a clear responsibility, but one business capability is spread across several packages. Product behavior, for example, is distributed among `controller`, `dto`, `entity`, `mapper`, `repository` and `service`.

This organization is valid for a small Spring Boot application, but it makes navigation harder as the number of capabilities grows.

## Request flow

The normal application flow is:

```text
Client
  |
  v
Controller
  |
  v
DTO validation
  |
  v
Service transaction
  |
  +--> Repository
  |      |
  |      v
  |    PostgreSQL
  |
  +--> Domain entity behavior
  |
  v
Mapper
  |
  v
Response DTO
```

Controllers are responsible for HTTP concerns. Services orchestrate use cases and transaction boundaries. Entities protect local domain invariants. Repositories encapsulate persistence access. Mappers prevent JPA entities from becoming the public API contract.

## Inventory flow

`StockService` is the main orchestration service in V1.

For an inventory movement, it performs the following sequence:

```text
resolve Product
    |
resolve Warehouse
    |
find or create Stock
    |
apply IN or OUT
    |
persist StockMovement
    |
check replenishment threshold
    |
create ReplenishmentOrder when required
```

The method is transactional, so the balance change, movement history and automatic replenishment decision are part of the same database transaction.

## Persistence model

The `stock` table has a unique constraint on `(product_id, warehouse_id)`. Therefore a product can have one stock balance in each warehouse, but there cannot be two independent balance rows for the same pair.

`Stock` also uses JPA `@Version` for optimistic locking. The repository currently also contains a locking strategy used during inventory modification. V2 must explicitly define and document which concurrency strategy is intended for each inventory operation.

Flyway owns schema evolution. Hibernate is configured with `ddl-auto: validate`, so entity mappings are checked against the database instead of silently modifying the schema.

## Transaction boundaries

Methods that modify state use `@Transactional`. Read operations use `@Transactional(readOnly = true)` when appropriate.

This distinction is especially important for inventory because a stock operation can touch multiple domain objects in one use case.

## V2 target organization

V2 will progressively evaluate organization by business capability rather than exclusively by technical layer.

```text
com.leandro.stockflow
    product
    warehouse
    inventory
    replenishment
    shared
```

The migration will be incremental. The project will not be reorganized in one large mechanical change. A feature will move only when its tests and API behavior remain stable.

The first feature used to establish the pattern will be Product because it has a small domain surface and exposes inconsistencies in the current create and update contracts.

## Architectural constraints for V2

The project will remain a modular monolith. V2 will not introduce microservices merely for portfolio complexity.

Controllers will not contain business rules. Repositories will not become service substitutes. JPA entities will not be returned directly from HTTP endpoints. Schema changes will continue to be performed exclusively through new Flyway migrations. Existing released migrations will not be edited after release.
