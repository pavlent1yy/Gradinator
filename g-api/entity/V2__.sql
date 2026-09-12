ALTER TABLE entry_subjects
DROP
CONSTRAINT fk5liu2li3vb2w8cd24ofo4tntd;

ALTER TABLE entry_teachers
DROP
CONSTRAINT fkfqeilyqn15j9esj35mnijooqy;

ALTER TABLE entry_rooms
DROP
CONSTRAINT fkm3afmi45pb5wfd7wy2agrbtjy;

CREATE TABLE entry_den_rooms
(
    entry_id BIGINT NOT NULL,
    room     VARCHAR(255)
);

CREATE TABLE entry_den_subjects
(
    entry_id BIGINT NOT NULL,
    subject  VARCHAR(255)
);

CREATE TABLE entry_den_teachers
(
    entry_id BIGINT NOT NULL,
    teacher  VARCHAR(255)
);

CREATE TABLE entry_num_rooms
(
    entry_id BIGINT NOT NULL,
    room     VARCHAR(255)
);

CREATE TABLE entry_num_subjects
(
    entry_id BIGINT NOT NULL,
    subject  VARCHAR(255)
);

CREATE TABLE entry_num_teachers
(
    entry_id BIGINT NOT NULL,
    teacher  VARCHAR(255)
);

ALTER TABLE schedule_entries
    ADD has_changes BOOLEAN;

ALTER TABLE schedule_entries
    ALTER COLUMN has_changes SET NOT NULL;

ALTER TABLE entry_den_rooms
    ADD CONSTRAINT fk_entry_den_rooms_on_schedule_entry FOREIGN KEY (entry_id) REFERENCES schedule_entries (id);

ALTER TABLE entry_den_subjects
    ADD CONSTRAINT fk_entry_den_subjects_on_schedule_entry FOREIGN KEY (entry_id) REFERENCES schedule_entries (id);

ALTER TABLE entry_den_teachers
    ADD CONSTRAINT fk_entry_den_teachers_on_schedule_entry FOREIGN KEY (entry_id) REFERENCES schedule_entries (id);

ALTER TABLE entry_num_rooms
    ADD CONSTRAINT fk_entry_num_rooms_on_schedule_entry FOREIGN KEY (entry_id) REFERENCES schedule_entries (id);

ALTER TABLE entry_num_subjects
    ADD CONSTRAINT fk_entry_num_subjects_on_schedule_entry FOREIGN KEY (entry_id) REFERENCES schedule_entries (id);

ALTER TABLE entry_num_teachers
    ADD CONSTRAINT fk_entry_num_teachers_on_schedule_entry FOREIGN KEY (entry_id) REFERENCES schedule_entries (id);

DROP TABLE entry_rooms CASCADE;

DROP TABLE entry_subjects CASCADE;

DROP TABLE entry_teachers CASCADE;