# StockFlow - V2 Roadmap

## Objective

Version 2 improves the quality of the existing system before adding broad new scope. Every change must strengthen at least one of these dimensions: domain correctness, maintainability, API quality, operational reliability, testability or portfolio value.

## Planned evolution

| Phase | Main outcome | Status |
|---|---|---|
| 0 | Freeze V1, start V2 branch, normalize line endings and establish documentation | Completed |
| 1 | Refactor Product contracts and establish the V2 package/code pattern | Completed |
| 2 | Improve Warehouse management API and contracts | Completed |
| 3 | Move inventory policy from global product minimum to product plus warehouse policy | In progress |
| 4 | Introduce reorder point and target stock | In progress |
| 5 | Implement atomic warehouse transfers | Planned |
| 6 | Complete replenishment lifecycle with cancellation and stock receiving | Planned |
| 7 | Add idempotency to critical inventory operations | Planned |
| 8 | Improve queries for low stock, products and warehouses | Planned |
| 9 | Strengthen exception handling, concurrency tests and integration tests | Planned |
| 10 | Add Actuator, CI and coverage reporting | Planned |
| 11 | Review whether authentication and authorization belong in V2 or V2.1 | Planned |
| 12 | Final regression, documentation review and release 2.0.0 | Planned |

## V2 domain changes under evaluation

The most important model change is inventory policy. V1 stores `minimumStock` on `Product`, while the balance itself belongs to `Product + Warehouse`. This means every warehouse is forced to use the same replenishment threshold.

V2 will migrate toward a per-location policy. The target model is conceptually:

```text
Product + Warehouse
    quantity
    reorderPoint
    targetStock
```

`reorderPoint` determines when replenishment should start. `targetStock` determines the desired balance after replenishment.

Example:

```text
Current quantity: 5
Reorder point: 10
Target stock: 30
Requested replenishment: 25
```

This is more realistic than replenishing only enough units to return exactly to the minimum threshold.

## Replenishment lifecycle

V1 defines `PENDING`, `COMPLETED` and `CANCELLED`, but only completion is exposed as a use case. Completion also changes only the order status and does not add inventory.

V2 will make the lifecycle operational. Receiving a replenishment must generate the corresponding inventory entry within the same transaction. Cancellation will become an explicit use case with business rules.

## Warehouse transfers

V2 will add a transfer operation that removes inventory from the source warehouse and adds inventory to the destination warehouse atomically.

The operation must be all-or-nothing. It will also provide a practical place to document transaction boundaries, locking order and concurrency behavior.

## Idempotency

Critical write operations will be evaluated for idempotency. A network retry must not accidentally apply the same inventory movement twice.

The existing movement `reference` field is not currently sufficient because uniqueness and request replay semantics are not enforced.

## Explicit non-goals for V2

The project will not be converted to microservices in V2. Kafka will not be introduced without a concrete asynchronous business requirement. Supplier management, full purchase-order management, financial inventory valuation and multi-tenancy are outside the initial V2 scope.

These boundaries exist to ensure that V2 reaches a coherent release instead of becoming an endless portfolio project.
