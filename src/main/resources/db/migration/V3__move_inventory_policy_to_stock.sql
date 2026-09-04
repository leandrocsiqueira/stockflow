ALTER TABLE stock
    ADD COLUMN reorder_point INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN target_stock INTEGER NOT NULL DEFAULT 0;

UPDATE stock s
SET reorder_point = p.minimum_stock,
    target_stock = p.minimum_stock
FROM products p
WHERE p.id = s.product_id;

ALTER TABLE stock
    ADD CONSTRAINT chk_stock_reorder_point_non_negative CHECK (reorder_point >= 0),
    ADD CONSTRAINT chk_stock_target_stock_non_negative CHECK (target_stock >= 0),
    ADD CONSTRAINT chk_stock_target_not_below_reorder CHECK (target_stock >= reorder_point);

ALTER TABLE products
    DROP CONSTRAINT IF EXISTS chk_products_minimum_stock_non_negative,
    DROP COLUMN minimum_stock;
