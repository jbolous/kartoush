ALTER TABLE customer
    ADD COLUMN password_hash VARCHAR(255) NOT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL,
    ADD COLUMN phone_number VARCHAR(30) NULL;

CREATE TABLE customer_address
(
    id                  VARCHAR(26) PRIMARY KEY,
    customer_id         VARCHAR(26)  NOT NULL,
    label               VARCHAR(100) NULL,
    line1               VARCHAR(150) NOT NULL,
    line2               VARCHAR(150) NULL,
    city                VARCHAR(100) NOT NULL,
    state_or_province   VARCHAR(100) NOT NULL,
    postal_code         VARCHAR(20)  NOT NULL,
    country_code        VARCHAR(2)   NOT NULL,
    type                VARCHAR(20)  NOT NULL,
    is_default_shipping BOOLEAN      NOT NULL DEFAULT FALSE,
    is_default_billing  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_customer_address_customer
        FOREIGN KEY (customer_id) REFERENCES customer (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_customer_address_customer_id ON customer_address (customer_id);

CREATE UNIQUE INDEX ux_customer_default_shipping
    ON customer_address (customer_id) WHERE is_default_shipping = TRUE;

CREATE UNIQUE INDEX ux_customer_default_billing
    ON customer_address (customer_id) WHERE is_default_billing = TRUE;
