# StockFlow - Idempotency

## Why it exists

Inventory writes are sensitive to retries. A browser, frontend, reverse proxy or API client may repeat a request after a timeout even when the first request was already committed. Without idempotency, a repeated IN movement could add stock twice and a repeated transfer could move the same quantity twice.

V2 protects externally initiated stock movements and warehouse transfers with an `Idempotency-Key` request header.

## Idempotency key versus reference

These concepts have different responsibilities.

`reference` is business metadata. Examples are a purchase order number, sales order number or transfer reference. Two movement rows created by one transfer intentionally share the same `reference`.

`Idempotency-Key` identifies one API execution attempt that may safely be retried. The same key may be replayed only with the same operation type and the same request payload.

Example:

```text
Idempotency-Key: 4d3f7aa0-72f1-4d73-b7a7-29df093d1210
```

## Protected endpoints

```text
POST /api/stock/movements
POST /api/stock/transfers
```

Both require the `Idempotency-Key` header.

## Persistence model

Flyway migration `V5__add_inventory_operation_idempotency.sql` creates `inventory_operations`.

Each row stores the idempotency key, operation type, SHA-256 fingerprint of the request, serialized successful response and creation timestamp.

The idempotency key is unique in PostgreSQL.

## Execution algorithm

At the beginning of the same transaction that will change stock, the application tries to reserve the key with:

```text
INSERT ... ON CONFLICT DO NOTHING
```

If the insert succeeds, the request owns the key and the inventory operation executes. The successful response is serialized into the idempotency record before commit.

If the key already exists, the application compares operation type and request fingerprint. When both match, the stored response is returned and no stock mutation is executed again.

If the key exists with a different operation or payload, the request is rejected with HTTP 409.

## Transaction behavior

The idempotency reservation participates in the same database transaction as the stock mutation.

If the inventory operation fails, the idempotency reservation rolls back as well. A later corrected retry may therefore use the key again because no successful operation was committed.

For simultaneous retries, PostgreSQL's unique-key conflict handling serializes reservation of the same key. Only one request performs the side effect. The other request observes the committed idempotency record and replays its stored response.

## Request fingerprint

The application creates a canonical representation of the fields that define each command and hashes it with SHA-256.

This prevents a caller from reusing the same key for a different quantity, warehouse, movement type, reason or business reference.

## Scope

The current V2 scope applies explicit request idempotency to manual stock movements and warehouse transfers. Replenishment receiving is protected by its state transition and row locking, which prevents the same pending order from being received twice. Broader command idempotency can be evaluated later if external integrations are added.
