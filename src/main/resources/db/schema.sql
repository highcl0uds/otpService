CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(10) NOT NULL
);

CREATE TABLE IF NOT EXISTS otp_config (
    id BIGSERIAL PRIMARY KEY,
    code_length INT NOT NULL DEFAULT 6,
    ttl_seconds INT NOT NULL DEFAULT 300
);

INSERT INTO otp_config(code_length, ttl_seconds)
SELECT 6, 300
WHERE NOT EXISTS (SELECT 1 FROM otp_config);

CREATE TABLE IF NOT EXISTS otp_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation_id VARCHAR(255) NOT NULL,
    code VARCHAR(20) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_otp_codes_status_expires ON otp_codes(status, expires_at);
