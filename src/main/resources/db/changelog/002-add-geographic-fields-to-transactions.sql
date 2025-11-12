-- liquibase formatted sql

-- changeset nickfallico:002-add-geographic-fields-to-transactions
-- comment: Add geographic location fields to transactions table for fraud detection

-- Add geographic location columns
ALTER TABLE transactions 
ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION,
ADD COLUMN IF NOT EXISTS country VARCHAR(2),
ADD COLUMN IF NOT EXISTS city VARCHAR(100),
ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45);

-- Add indexes for geographic queries (used by fraud detection rules)
CREATE INDEX IF NOT EXISTS idx_transactions_user_country 
ON transactions(user_id, country) 
WHERE country IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transactions_created_at 
ON transactions(created_at);

CREATE INDEX IF NOT EXISTS idx_transactions_user_created_location 
ON transactions(user_id, created_at, latitude, longitude) 
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

-- Add comment to document the schema change
COMMENT ON COLUMN transactions.latitude IS 'Geographic latitude coordinate (-90 to 90)';
COMMENT ON COLUMN transactions.longitude IS 'Geographic longitude coordinate (-180 to 180)';
COMMENT ON COLUMN transactions.country IS 'ISO 3166-1 alpha-2 country code (e.g., US, GB, FR)';
COMMENT ON COLUMN transactions.city IS 'City name where transaction originated';
COMMENT ON COLUMN transactions.ip_address IS 'IP address of transaction origin (IPv4 or IPv6)';

-- rollback ALTER TABLE transactions DROP COLUMN IF EXISTS latitude, DROP COLUMN IF EXISTS longitude, DROP COLUMN IF EXISTS country, DROP COLUMN IF EXISTS city, DROP COLUMN IF EXISTS ip_address;
-- rollback DROP INDEX IF EXISTS idx_transactions_user_country;
-- rollback DROP INDEX IF EXISTS idx_transactions_user_created_location;