ALTER TABLE customer_credential
    RENAME TO customer_password;

ALTER INDEX idx_customer_credential_created_at
    RENAME TO idx_customer_password_created_at;
