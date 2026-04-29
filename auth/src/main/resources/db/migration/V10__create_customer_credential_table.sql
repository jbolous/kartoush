CREATE TABLE customer_credential
(
    customer_id   VARCHAR(26) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_credential_created_at
    ON customer_credential (created_at);
