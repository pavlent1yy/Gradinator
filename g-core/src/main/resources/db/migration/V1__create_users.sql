CREATE TABLE "User"
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(50)  NOT NULL DEFAULT 'STUDENT',
    registered_at TIMESTAMPTZ           DEFAULT now(),
    updated_at    TIMESTAMPTZ,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE
);