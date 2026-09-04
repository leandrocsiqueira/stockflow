# StockFlow - Business Rules

## Purpose

This document is the quickest reference for understanding what the backend is allowed to do. Read it after `02-domain-model.md` and before changing services. The code remains the executable source of truth, while this document explains the intent behind the rules.

## Product catalog

A product is identified by a unique SKU. SKU and unit are defined at creation and are not changed by the current update use case. Product name is mutable. Inventory thresholds do not belong to Product because inventory policy varies by warehouse.

## Warehouse catalog

A warehouse represents one inventory location. A stock balance always belongs to one Product + Warehouse pair. Warehouse activation and deactivation are not implemented yet because that lifecycle must define what happens to existing balances, transfers and replenishment orders.

## Inventory balance

Stock quantity can never become negative. Every stock increase or decrease must use a positive amount. An OUT movement or transfer is rejected when the source balance is insufficient.

The current quantity is stored in `stock` for efficient reads. The corresponding business history is stored in `stock_movements`.

## Inventory policy

Each Product + Warehouse stock record has `reorderPoint` and `targetStock`.

`reorderPoint` cannot be negative. `targetStock` cannot be lower than `reorderPoint`. A zero/zero policy disables automatic replenishment.

Automatic replenishment is triggered only when current quantity is strictly lower than the reorder point.

```text
if quantity < reorderPoint:
    requestedQuantity = targetStock - quantity
```

Only one PENDING replenishment order may exist for the same Product + Warehouse pair. This rule is protected both in application code and by a partial unique database index.

## Manual stock movement

An `IN` movement increases stock. An `OUT` movement decreases stock. The service locks the affected stock row before changing the balance so concurrent writes do not overwrite each other casually.

After a movement, the inventory policy is reevaluated. If the resulting balance is below the reorder point and no replenishment is already pending, a new replenishment order is created.

## Warehouse transfer

A transfer moves one product from one warehouse to another. Source and destination must be different. The source must have enough quantity.

A transfer is atomic. The source decrease, destination increase, OUT history record and IN history record belong to the same database transaction. If any step fails, none of the transfer is committed.

Both movement records share the same external transfer reference. Existing stock locks are acquired in deterministic warehouse-id order to reduce deadlock risk for inverse concurrent transfers.

After the transfer, replenishment policy is reevaluated for both warehouse balances.

## Replenishment creation

A replenishment order starts with status `PENDING`. Its requested quantity is calculated when the order is created and represents the amount required to reach target stock at that moment.

The creation timestamp is supplied by the application `Clock` in service-level use cases, which makes time-dependent behavior deterministic in tests.

## Replenishment cancellation

Only a `PENDING` replenishment order can be cancelled. Cancellation changes its status to `CANCELLED`, records `cancelledAt` and does not change stock quantity or movement history.

A completed replenishment cannot be cancelled.

## Replenishment receiving

Only a `PENDING` replenishment order can be received. The order is locked before lifecycle processing so competing receive/cancel operations cannot both succeed.

Receiving performs one stock increase using the order's requested quantity and records one `IN` movement with reference `REPLENISHMENT-{orderId}`. The same transaction then changes the order to `COMPLETED` and records `completedAt`.

A second receive attempt against the same order is rejected before another stock change is made.

If the inventory policy changed while the order was pending and the received quantity still leaves stock below the current reorder point, the completed order is flushed and a new pending replenishment is created for the remaining amount required to reach the new target stock.

## Transaction boundaries

Stock movement, warehouse transfer and replenishment receiving are transactional write use cases. The database transaction is part of the business guarantee rather than only an implementation detail.

Controller tests verify HTTP contracts. Service and entity tests verify local business decisions. The Testcontainers integration suite verifies that transactional behavior, Flyway migrations and PostgreSQL constraints work together against a real database engine.
