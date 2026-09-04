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

### Next stage

After this stage is green and committed, the project moves into its first substantial domain migration: inventory policy will stop being globally attached to `Product` and will become specific to each product and warehouse combination.

