# StockFlow - V2 Development Log

## How to use this document

This file records why important changes were made, which files or modules were affected and how each step was validated. Its purpose is to make the project understandable after a long break and to provide enough context to continue development in another conversation without reconstructing prior decisions.

## Stage 0 - V2 baseline

### Starting point

The uploaded project is on the `main` branch. The last committed V1 revision is:

```text
6315edc feat: improve API documentation, errors and controller tests
```

The project Maven version at that revision is `1.0.0`. The existing Surefire reports record 49 tests, with 0 failures, 0 errors and 0 skipped tests.

No Git tag existed in the uploaded repository.

### Line ending observation

The uploaded ZIP initially appeared to contain 11 modified files. A Git comparison with end-of-line whitespace ignored showed no semantic differences. The changes were caused exclusively by LF and CRLF conversion.

V2 therefore introduces `.gitattributes` so source-controlled text files use predictable line endings regardless of the developer operating system.

### Version strategy

The V1 commit is preserved with tag:

```text
v1.0.0
```

V2 development starts from that exact commit on branch:

```text
v2
```

The Maven project version becomes:

```text
2.0.0-SNAPSHOT
```

The final V2 release will change the Maven version to `2.0.0` and receive a corresponding Git tag.

### Documentation introduced

| File | Purpose |
|---|---|
| `docs/00-project-overview.md` | Provides a fast mental model of the system |
| `docs/01-architecture.md` | Explains layers, request flow, transactions and target organization |
| `docs/07-v2-roadmap.md` | Defines V2 scope and stopping point |
| `docs/08-development-log.md` | Records the reasoning and validation behind each development stage |

### Validation

This stage changes no Java behavior or database schema. Validation consists of checking Git differences, validating that only intentional documentation/versioning files changed and running the existing test suite in the developer environment before the first V2 commit.

### Next stage

Stage 1 will refactor Product API contracts. `ProductRequest` is currently reused for create and update even though `ProductService.update()` modifies only `name` and `minimumStock`. The V2 contract will make writable fields explicit and tests will be updated before any broader package reorganization.

## Stage 1 - Product API contracts

### Problem

V1 used `ProductRequest` for both product creation and product update. The request required `sku`, `name`, `unit` and `minimumStock`, but `ProductService.update()` changed only `name` and `minimumStock`. As a result, the HTTP update contract required fields that the application intentionally ignored.

### Decision

Product creation and update now use different request DTOs. `sku` and `unit` are treated as immutable catalog identifiers after creation. This makes the API contract match the actual use case and prevents clients from believing these values can be changed through `PUT /api/products/{id}`.

### Changes

`ProductRequest` was replaced by `CreateProductRequest` and `UpdateProductRequest`. The controller and service now expose the specific contract required by each operation. The OpenAPI update description explicitly states that SKU and unit are immutable.

Controller tests now cover the update endpoint and update validation. A new `ProductServiceTest` verifies creation, duplicate-SKU rejection, mutable-field updates and not-found behavior.

### API examples

Create request:

```json
{
  "sku": "SKU-001",
  "name": "Keyboard",
  "unit": "UN",
  "minimumStock": 5
}
```

Update request:

```json
{
  "name": "Mechanical Keyboard",
  "minimumStock": 10
}
```

### Validation

Run the full Maven test suite with Docker available because `StockMovementIntegrationTest` uses Testcontainers.

```text
mvn clean test
```

### Validation result

Stage 1 was validated successfully in the developer environment.

```text
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Next stage

Warehouse receives the same contract cleanup before larger domain changes begin.

## Stage 2 - Warehouse API completeness

### Problem

V1 exposed only warehouse creation and full listing. There was no endpoint to retrieve one warehouse or update its catalog data. The same `WarehouseRequest` type also represented every write operation, making the contract less explicit than the Product contract established in Stage 1.

### Decision

Warehouse creation and update use separate request DTOs. The API now supports retrieving and updating a warehouse by ID. This makes the warehouse catalog usable by the frontend without adding lifecycle state prematurely.

An `active` flag is intentionally not introduced in this stage. Disabling a warehouse has consequences for stock movements, transfers and replenishment. That rule will be introduced only when inventory policy is defined so the application does not expose a half-implemented lifecycle.

### Changes

`WarehouseRequest` is replaced by `CreateWarehouseRequest` and `UpdateWarehouseRequest`. `WarehouseService` adds `findById()` and `update()`, and `WarehouseController` exposes `GET /api/warehouses/{id}` and `PUT /api/warehouses/{id}`.

Controller and service tests are added for creation, retrieval, update, validation and not-found behavior. The stock movement integration test is updated to use the new warehouse creation contract.

### API examples

Create request:

```json
{
  "name": "Main Warehouse",
  "location": "SP"
}
```

Update request:

```json
{
  "name": "Distribution Center",
  "location": "MG"
}
```

### Validation

Run the complete Maven test suite with Docker available.

```text
mvn clean test
```

### Validation result

Stage 2 was validated successfully in the developer environment.

```text
Tests run: 65, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Next stage

The project now moves into its first substantial domain migration: inventory policy stops being globally attached to `Product` and becomes specific to each product and warehouse combination.

## Stage 3 - Per-warehouse inventory policy

### Problem

V1 stored `minimumStock` on `Product`. Stock quantity, however, already belonged to a Product + Warehouse pair. This forced every warehouse to use the same replenishment threshold for a product and made the catalog entity responsible for an operational inventory rule.

### Decision

Inventory policy moves to `Stock`, the entity that represents one product in one warehouse. `Product` now contains only catalog information. Each stock record owns `reorderPoint` and `targetStock`.

`reorderPoint` determines when replenishment starts. `targetStock` determines the desired quantity after replenishment and must be greater than or equal to the reorder point. A zero/zero policy means automatic replenishment is disabled for that location.

### Database migration

Flyway migration `V3__move_inventory_policy_to_stock.sql` adds `reorder_point` and `target_stock` to `stock`. Existing stock rows inherit the old product minimum for both values, preserving V1 replenishment behavior for data that already has a stock record. The obsolete `products.minimum_stock` column is then removed.

### API changes

Product creation no longer accepts `minimumStock` and product update now changes only `name`. Inventory policy is configured explicitly with:

```text
PUT /api/stock/policies
```

Example:

```json
{
  "productId": 1,
  "warehouseId": 1,
  "reorderPoint": 10,
  "targetStock": 30
}
```

The stock response now exposes quantity, reorder point, target stock and whether the current balance is below the reorder point.

### Replenishment calculation

V1 requested only enough units to return to the global minimum. V2 requests enough units to reach target stock.

```text
quantity = 5
reorderPoint = 10
targetStock = 30
requestedQuantity = 25
```

This policy is evaluated independently for each warehouse.

### Tests

Entity tests cover policy validation and target-stock calculation. Controller tests cover the new policy endpoint and cross-field validation. The Testcontainers integration suite verifies that different warehouses can use different policies and that replenishment requests the quantity required to reach target stock.

### Validation

Run the complete Maven test suite with Docker available.

```text
mvn clean test
```

### Next stage

After this migration is green, the next major use case is an atomic warehouse transfer. It will move the same product between two warehouses in one transaction and record both sides of the operation in movement history.

## Stage 4 - Atomic warehouse transfers

### Problem

V1 could move inventory between warehouses only by issuing an OUT movement and a separate IN movement. If one operation succeeded and the other failed, the balances could diverge and the history would not represent one atomic business operation.

### Decision

V2 introduces `POST /api/stock/transfers`. A transfer moves one product from a source warehouse to a destination warehouse inside one database transaction. Source and destination must be different, quantity must be positive, and the source must have enough stock.

Both movement records share the same required `reference`, which correlates the OUT and IN sides in movement history. Reference uniqueness is intentionally deferred to the idempotency stage.

### Concurrency strategy

When both stock records already exist, they are locked in ascending warehouse-id order rather than request order. This deterministic order reduces deadlock risk for concurrent inverse transfers such as warehouse A to B and warehouse B to A. A destination stock record is created when the product has never existed there.

### Transactional behavior

The source balance is decreased and the destination balance is increased in the same transaction. The service then records one OUT movement for the source and one IN movement for the destination using the same timestamp and reference. If any business rule fails, the transaction rolls back.

After the balance change, replenishment policy is reevaluated for both locations. This allows an outgoing transfer to trigger replenishment at the source if its remaining quantity falls below the reorder point.

### API example

```json
{
  "productId": 1,
  "sourceWarehouseId": 1,
  "destinationWarehouseId": 2,
  "quantity": 20,
  "reason": "Internal transfer",
  "reference": "TRF-001"
}
```

The response exposes both generated movement IDs, both resulting balances and the common transfer reference.

### Tests

Controller tests verify the HTTP contract and validation of the required reference. Testcontainers integration tests verify successful transfer, rollback on insufficient stock, rejection of a same-warehouse transfer and automatic source replenishment after a transfer.

### Validation

Run the complete Maven test suite with Docker available.

```text
mvn clean test
```

### Next stage

The next stage completes the replenishment lifecycle. Pending orders will support cancellation and receiving. Receiving a replenishment will create the physical IN movement and complete the order in the same transaction.

## Stage 5 - Operational replenishment lifecycle

### Problem

V1 exposed `PENDING`, `COMPLETED` and `CANCELLED`, but only completion had a service operation. Completion merely changed status and did not add the replenished quantity to inventory. As a result, the order lifecycle and physical stock history could diverge.

The entity also created lifecycle timestamps internally with `LocalDateTime.now()`, while stock operations already used the application `Clock`. That made time handling inconsistent and less deterministic in tests.

### Decision

The vague `complete` API is replaced by explicit receiving and cancellation use cases. Receiving means physical inventory arrived. It therefore increases Stock, records an IN StockMovement and transitions the replenishment order to COMPLETED in one transaction.

Cancellation is permitted only for a pending order and never changes stock.

Replenishment lifecycle operations lock the order row with `PESSIMISTIC_WRITE` so competing receive and cancel requests cannot both succeed against the same pending state.

### Database migration

Flyway migration `V4__complete_replenishment_lifecycle.sql` adds `cancelled_at`. Existing terminal rows are backfilled when necessary and a database check constraint enforces timestamp/status consistency for PENDING, COMPLETED and CANCELLED states.

### API changes

The old endpoint:

```text
POST /api/replenishment-orders/{id}/complete
```

is replaced by:

```text
POST /api/replenishment-orders/{id}/receive
POST /api/replenishment-orders/{id}/cancel
```

Receiving returns the order id, generated stock movement id, received quantity, resulting balance, generated reference and receipt timestamp.

The generated movement reference uses:

```text
REPLENISHMENT-{orderId}
```

### Time handling

`ReplenishmentOrder` now accepts explicit lifecycle timestamps. Service use cases supply timestamps from the application `Clock`. The convenience constructor remains available for direct entity construction, while application behavior no longer depends on an uncontrolled system clock for replenishment creation, receiving or cancellation.

### Policy reevaluation after receiving

Normally, receiving the requested quantity reaches the target stock that existed when the order was generated. However, policy may change while the order is pending. If the resulting quantity remains below the current reorder point, the completed order is flushed and a new pending order is generated from the current policy.

### Tests

Entity tests cover terminal state transitions and invalid repeated transitions. Controller tests cover the receive and cancel HTTP contracts and conflict responses. Service tests verify deterministic timestamps, stock receiving and rejection before stock mutation. Testcontainers integration tests verify receiving into physical stock, the generated movement history, cancellation without balance changes and rejection of receiving a cancelled order.

### Validation

Run the complete Maven test suite with Docker available.

```text
mvn clean test
```

Validation result is recorded after the suite succeeds in the developer environment.

### Next stage

Stage 6 adds idempotency to externally initiated critical inventory operations. The goal is to make safe client retries explicit instead of treating `reference` only as descriptive metadata.


## Stage 6 - Idempotent inventory commands

### Problem

A client retry could previously execute a successful stock movement or warehouse transfer more than once. The existing `reference` field could not safely solve this because it is business metadata and a transfer intentionally produces two movements with the same reference.

### Decision

`POST /api/stock/movements` and `POST /api/stock/transfers` now require an `Idempotency-Key` header. The API stores a dedicated inventory-operation record instead of overloading business references with transport-level retry semantics.

### Database migration

Flyway migration `V5__add_inventory_operation_idempotency.sql` creates `inventory_operations`. The table has a unique idempotency key, operation type, SHA-256 request hash, serialized successful response and creation timestamp.

### Concurrency strategy

The key is reserved with PostgreSQL `INSERT ... ON CONFLICT DO NOTHING` before any stock mutation. The reservation belongs to the same transaction as the movement or transfer. Concurrent requests using the same key are therefore serialized by the database uniqueness constraint.

If the first transaction commits, a retry replays the persisted response. If the first transaction rolls back, its reservation rolls back and a later request can execute normally.

### Payload protection

A canonical representation of the request is hashed with SHA-256. Reusing a key with a different request or with a different operation type produces a business conflict instead of silently replaying unrelated work.

### API behavior

`reference` remains unchanged as business metadata. `Idempotency-Key` is supplied through the HTTP header and has a maximum length of 100 characters. Successful retries return the same serialized response generated by the original operation and do not change balances or movement history again.

### Tests

Controller tests verify that the header is forwarded and required. Testcontainers integration tests verify movement replay, transfer replay and rejection of the same key with a changed payload.

### Validation

Run the complete Maven test suite with Docker available.

```text
mvn clean test
```

### Next stage

Stage 7 improves read-side usability with low-stock queries and stronger product and warehouse search/pagination.
