CREATE TABLE customer_password_setup_token
(
    id          VARCHAR(26) PRIMARY KEY,
    customer_id VARCHAR(26)              NOT NULL,
    token_hash  VARCHAR(128)             NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_customer_password_setup_token_customer_id
    ON customer_password_setup_token (customer_id);
