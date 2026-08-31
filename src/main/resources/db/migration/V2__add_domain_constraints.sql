ALTER TABLE products ADD CONSTRAINT chk_products_minimum_stock_non_negative CHECK (minimum_stock >= 0);

ALTER TABLE stock_movements ADD CONSTRAINT chk_stock_movements_type CHECK (
	type IN (
		'IN'
		,'OUT'
		)
	);

ALTER TABLE replenishment_orders ADD CONSTRAINT chk_replenishment_orders_status CHECK (
	STATUS IN (
		'PENDING'
		,'COMPLETED'
		,'CANCELLED'
		)
	);

CREATE UNIQUE INDEX uq_replenishment_pending_product_warehouse ON replenishment_orders (
	product_id
	,warehouse_id
	)
WHERE STATUS = 'PENDING';
