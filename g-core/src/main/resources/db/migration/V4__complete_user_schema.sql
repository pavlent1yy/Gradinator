ALTER TABLE "User"
    ADD COLUMN IF NOT EXISTS department VARCHAR(255);

ALTER TABLE "User"
    ADD COLUMN IF NOT EXISTS group_name VARCHAR(255);

CREATE TABLE IF NOT EXISTS refresh_sessions
(
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT       NOT NULL REFERENCES "User" (id),
    refresh_token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at         TIMESTAMPTZ  NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    revoked_at         TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_refresh_sessions_user_id
    ON refresh_sessions (user_id);
