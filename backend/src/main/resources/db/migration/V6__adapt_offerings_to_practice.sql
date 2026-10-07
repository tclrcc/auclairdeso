-- Adjustments after the interview with the practitioner (docs/specification.md, section 3).

-- No written sessions; sessions at the client's home instead.
DELETE FROM offering_mode WHERE mode = 'WRITTEN';
ALTER TABLE offering_mode DROP CONSTRAINT offering_mode_value_ck;
ALTER TABLE offering_mode ADD CONSTRAINT offering_mode_value_ck
    CHECK (mode IN ('IN_PERSON', 'CLIENT_HOME', 'VIDEO', 'PHONE'));

-- Break after each session, specific to each offering.
ALTER TABLE offering ADD COLUMN buffer_minutes INTEGER NOT NULL DEFAULT 0;
ALTER TABLE offering ADD CONSTRAINT offering_buffer_ck CHECK (buffer_minutes BETWEEN 0 AND 120);

-- Her rule for the existing offerings: 30 minutes after a short session, 15 after a longer one.
UPDATE offering SET buffer_minutes = CASE WHEN duration_minutes <= 30 THEN 30 ELSE 15 END;
