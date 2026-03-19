-- 1. Set default value for new rows
ALTER TABLE customer
    ALTER COLUMN status SET DEFAULT 'PENDING';

-- 2. Backfill any existing NULL values (just in case)
UPDATE customer
SET status = 'PENDING'
WHERE status IS NULL;

-- 3. Drop existing constraint if it exists (safe reruns)
ALTER TABLE customer
    DROP CONSTRAINT IF EXISTS chk_customer_status;

-- 4. Don't allow NULL values for
ALTER TABLE customer
    ALTER COLUMN status SET NOT NULL;

-- 5. Add constraint for allowed values
ALTER TABLE customer
    ADD CONSTRAINT chk_customer_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'DELETED'));

