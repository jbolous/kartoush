ALTER TABLE customer
    DROP CONSTRAINT chk_customer_status;

ALTER TABLE customer
    ADD CONSTRAINT chk_customer_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'DELETED'));
