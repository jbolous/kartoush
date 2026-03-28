-- V2__create_customer_table.sql

CREATE TABLE customer
(
    id         VARCHAR(26) PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT customer_email_unique UNIQUE (email)
);

CREATE INDEX customer_created_at_idx ON customer (created_at);
