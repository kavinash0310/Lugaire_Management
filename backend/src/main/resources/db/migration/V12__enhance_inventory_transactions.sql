ALTER TABLE inventory_transactions
    ADD COLUMN IF NOT EXISTS previous_stock INTEGER,
    ADD COLUMN IF NOT EXISTS new_stock INTEGER,
    ADD COLUMN IF NOT EXISTS recorded_by_user_id BIGINT,
    ADD COLUMN IF NOT EXISTS recorded_by_user_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS remarks TEXT;

CREATE INDEX IF NOT EXISTS idx_inventory_transactions_variant_created_at
    ON inventory_transactions(variant_id, created_at DESC);
