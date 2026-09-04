CREATE TABLE inventory_operations (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    operation_type VARCHAR(32) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_operations_type CHECK (operation_type IN ('MOVEMENT', 'TRANSFER')),
    CONSTRAINT chk_inventory_operations_key_not_blank CHECK (length(trim(idempotency_key)) > 0),
    CONSTRAINT chk_inventory_operations_hash_length CHECK (length(request_hash) = 64)
);

CREATE INDEX idx_inventory_operations_created_at ON inventory_operations(created_at);
