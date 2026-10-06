CREATE TABLE IF NOT EXISTS absence
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT      NOT NULL REFERENCES "User" (id) ON DELETE CASCADE,
    absence_date DATE        NOT NULL,
    pair_number  INTEGER     NOT NULL,
    type         VARCHAR(20) NOT NULL,
    subject      VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_absence_user_date_pair UNIQUE (user_id, absence_date, pair_number)
);
