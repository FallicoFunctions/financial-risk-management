--liquibase formatted sql

--changeset nickfallico:create-transactions-table
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    transaction_type VARCHAR(50),
    merchant_category VARCHAR(255),
    merchant_name VARCHAR(255),
    is_international BOOLEAN
);

--changeset nickfallico:create-transaction-indexes
CREATE INDEX idx_user_created ON transactions (user_id, created_at);
CREATE INDEX idx_merchant ON transactions (merchant_category);