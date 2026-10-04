-- OAuth-пользователи создаются без пароля
ALTER TABLE "User"
    ALTER COLUMN password_hash DROP NOT NULL;

CREATE TABLE IF NOT EXISTS user_oauth_account
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES "User" (id) ON DELETE CASCADE,
    provider         VARCHAR(30)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL
);

-- Таблица могла уже существовать (создана Hibernate через ddl-auto=update), поэтому ограничение отдельно
DO
$$
    BEGIN
        IF NOT EXISTS (SELECT 1
                       FROM pg_constraint
                       WHERE conname = 'uk_oauth_provider_user') THEN
            ALTER TABLE user_oauth_account
                ADD CONSTRAINT uk_oauth_provider_user UNIQUE (provider, provider_user_id);
        END IF;
    END
$$;

CREATE INDEX IF NOT EXISTS idx_user_oauth_account_user_id
    ON user_oauth_account (user_id);
