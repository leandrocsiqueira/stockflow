ALTER TABLE replenishment_orders
    ADD COLUMN cancelled_at TIMESTAMP;

UPDATE replenishment_orders
SET completed_at = NULL,
    cancelled_at = NULL
WHERE status = 'PENDING';

UPDATE replenishment_orders
SET completed_at = COALESCE(completed_at, created_at),
    cancelled_at = NULL
WHERE status = 'COMPLETED';

UPDATE replenishment_orders
SET completed_at = NULL,
    cancelled_at = COALESCE(cancelled_at, created_at)
WHERE status = 'CANCELLED';

ALTER TABLE replenishment_orders
    ADD CONSTRAINT chk_replenishment_terminal_timestamp CHECK (
        (status = 'PENDING' AND completed_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND completed_at IS NULL AND cancelled_at IS NOT NULL)
    );
