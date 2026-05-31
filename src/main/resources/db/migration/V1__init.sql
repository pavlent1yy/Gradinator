CREATE TABLE group_ (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE week_day (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE week_type (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(255) NOT NULL UNIQUE
);
CREATE TABLE schedule (
                          id BIGSERIAL PRIMARY KEY,

                          pair_number INTEGER NOT NULL,

                          group_id BIGINT REFERENCES group_(id),
                          weekday_id BIGINT REFERENCES week_day(id),
                          weektype_id BIGINT REFERENCES week_type(id),

                          room VARCHAR(255) NOT NULL,
                          subject VARCHAR(255) NOT NULL,
                          teacher VARCHAR(255) NOT NULL
);
CREATE TABLE user_ (
                       id BIGSERIAL PRIMARY KEY,

                       group_id BIGINT REFERENCES group_(id),

                       registered_at TIMESTAMPTZ DEFAULT now(),

                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL
);