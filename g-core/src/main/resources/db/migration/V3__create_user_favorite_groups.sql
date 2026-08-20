CREATE TABLE user_favorite_groups
(
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT       NOT NULL,
    group_name VARCHAR(50) NOT NULL,

    CONSTRAINT fk_favorite_groups_settings
        FOREIGN KEY (user_id)
            REFERENCES user_settings (user_id)
            ON DELETE CASCADE,

    CONSTRAINT uq_user_favorite_group
        UNIQUE (user_id, group_name)
);