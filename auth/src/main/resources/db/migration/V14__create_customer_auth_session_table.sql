CREATE TABLE customer_auth_session
(
    id         VARCHAR(26) PRIMARY KEY,
    customer_id VARCHAR(26) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    issued_at  TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_customer_auth_session_customer_id
    ON customer_auth_session (customer_id);
