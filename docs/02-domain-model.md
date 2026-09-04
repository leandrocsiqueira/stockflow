# StockFlow - Domain Model

## Purpose

This document explains the StockFlow domain in business terms. It should be read before diving into controllers or repositories because the most important application rules are expressed by the relationships between Product, Warehouse, Stock, StockMovement and ReplenishmentOrder.

## Product

`Product` represents an item in the inventory catalog.

Its main data is SKU, name and unit. SKU identifies the item and must be unique. SKU and unit are treated as immutable after creation. Product does not define replenishment thresholds because the same item can require different inventory levels in different warehouses.

## Warehouse

`Warehouse` represents a physical or logical inventory location.

A product can exist in many warehouses and each warehouse can contain many products. StockFlow therefore does not store quantity directly on Product or Warehouse.

## Stock

`Stock` represents one Product + Warehouse combination.

```text
Stock
    product
    warehouse
    quantity
    reorderPoint
    targetStock
    version
```

`quantity` is the current physical balance.

`reorderPoint` is the threshold that starts replenishment. Replenishment is required only when the quantity becomes strictly lower than this value.

`targetStock` is the balance the system wants to reach through a replenishment request. It must be greater than or equal to `reorderPoint`.

A policy of `reorderPoint = 0` and `targetStock = 0` effectively disables automatic replenishment for that Product + Warehouse combination.

Example:

```text
Product: Keyboard
Warehouse: Main Warehouse
Quantity: 5
Reorder point: 10
Target stock: 30

Requested replenishment = 30 - 5 = 25
```

The same product may use another policy elsewhere:

```text
Product: Keyboard
Warehouse: Secondary Warehouse
Quantity: 5
Reorder point: 4
Target stock: 12
```

This is the central V2 domain correction. V1 stored one global minimum on Product, which could not represent different operating requirements between locations.

## StockMovement

`StockMovement` is the immutable history of inventory changes.

An `IN` movement adds quantity and an `OUT` movement removes quantity. An OUT operation is rejected if it would make the balance negative.

The current balance can therefore be understood as the result of the movement history, while the Stock row provides the efficiently accessible current state.

## ReplenishmentOrder

`ReplenishmentOrder` represents a replenishment request created when inventory falls below the configured reorder point.

Only one pending order may exist for the same Product + Warehouse pair. The requested quantity is calculated from target stock instead of merely returning the balance to the threshold.

```text
if quantity < reorderPoint:
    requestedQuantity = targetStock - quantity
```

The later V2 replenishment stage will make receiving and cancellation operational. At the current stage, automatic creation is already based on the new per-location inventory policy.

## Aggregate behavior

The important consistency boundary is the Product + Warehouse stock record. Inventory writes use a locking strategy around this record so concurrent operations cannot casually overwrite the same balance.

Future warehouse transfers will coordinate two such stock records inside one transaction and will use a deterministic locking order to reduce deadlock risk.
