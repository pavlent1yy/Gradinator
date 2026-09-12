CREATE TABLE groups (
                        name VARCHAR(255) NOT NULL,
                        xlsx_file VARCHAR(255),
                        CONSTRAINT groups_pkey PRIMARY KEY (name)
);


CREATE TABLE heartbeat_logs (
                                id BIGINT NOT NULL,
                                started_at TIMESTAMP NOT NULL,
                                finished_at TIMESTAMP,
                                message VARCHAR(255),
                                status VARCHAR(255),
                                CONSTRAINT heartbeat_logs_pkey PRIMARY KEY (id)
);


CREATE TABLE schedule_files (
                                id BIGINT NOT NULL,
                                filename VARCHAR(255) NOT NULL,
                                size BIGINT,
                                updated_at TIMESTAMP,
                                hash VARCHAR(255),
                                etag VARCHAR(255),
                                CONSTRAINT schedule_files_pkey PRIMARY KEY (id),
                                CONSTRAINT schedule_files_filename_key UNIQUE (filename)
);


CREATE TABLE schedule_snapshots (
                                    id BIGINT NOT NULL,
                                    schedule_date DATE NOT NULL,
                                    created_at TIMESTAMP NOT NULL,
                                    hash VARCHAR(255) NOT NULL,
                                    CONSTRAINT schedule_snapshots_pkey PRIMARY KEY (id)
);


CREATE TABLE schedule_entries (
                                  id BIGINT NOT NULL,
                                  snapshot_id BIGINT NOT NULL,
                                  pair_number INTEGER NOT NULL,
                                  day VARCHAR(255) NOT NULL,
                                  group_name VARCHAR(255) NOT NULL,
                                  has_changes BOOLEAN NOT NULL,

                                  CONSTRAINT schedule_entries_pkey PRIMARY KEY (id),
                                  CONSTRAINT fk_schedule_entries_snapshot
                                      FOREIGN KEY (snapshot_id)
                                          REFERENCES schedule_snapshots(id)
);


CREATE TABLE entry_num_subjects (
                                    entry_id BIGINT NOT NULL,
                                    subject VARCHAR(255),

                                    CONSTRAINT fk_entry_num_subjects
                                        FOREIGN KEY (entry_id)
                                            REFERENCES schedule_entries(id)
);


CREATE TABLE entry_num_teachers (
                                    entry_id BIGINT NOT NULL,
                                    teacher VARCHAR(255),

                                    CONSTRAINT fk_entry_num_teachers
                                        FOREIGN KEY (entry_id)
                                            REFERENCES schedule_entries(id)
);


CREATE TABLE entry_num_rooms (
                                 entry_id BIGINT NOT NULL,
                                 room VARCHAR(255),

                                 CONSTRAINT fk_entry_num_rooms
                                     FOREIGN KEY (entry_id)
                                         REFERENCES schedule_entries(id)
);


CREATE TABLE entry_den_subjects (
                                    entry_id BIGINT NOT NULL,
                                    subject VARCHAR(255),

                                    CONSTRAINT fk_entry_den_subjects
                                        FOREIGN KEY (entry_id)
                                            REFERENCES schedule_entries(id)
);


CREATE TABLE entry_den_teachers (
                                    entry_id BIGINT NOT NULL,
                                    teacher VARCHAR(255),

                                    CONSTRAINT fk_entry_den_teachers
                                        FOREIGN KEY (entry_id)
                                            REFERENCES schedule_entries(id)
);


CREATE TABLE entry_den_rooms (
                                 entry_id BIGINT NOT NULL,
                                 room VARCHAR(255),

                                 CONSTRAINT fk_entry_den_rooms
                                     FOREIGN KEY (entry_id)
                                         REFERENCES schedule_entries(id)
);