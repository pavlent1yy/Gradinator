CREATE TABLE user_settings
(
    user_id               BIGINT PRIMARY KEY,
    theme                 VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    current_group         VARCHAR(50),

    notifications_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    show_changes          BOOLEAN     NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_user_settings_user
        FOREIGN KEY (user_id)
            REFERENCES "User" (id)
            ON DELETE CASCADE
);