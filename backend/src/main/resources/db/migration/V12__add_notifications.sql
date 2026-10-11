-- Notifications (docs/specification.md, 7).

-- The reminder of the day before leaves once per confirmed session.
ALTER TABLE appointment ADD COLUMN reminder_sent_at TIMESTAMP WITH TIME ZONE;

-- Events waiting for their listeners: the Spring Modulith event publication registry (JDBC, schema v2).
-- Copied from org/springframework/modulith/events/jdbc/schemas/v2/schema-postgresql.sql, version 2.1.1.
CREATE TABLE event_publication (
                                   id                     UUID                     NOT NULL,
                                   listener_id            TEXT                     NOT NULL,
                                   event_type             TEXT                     NOT NULL,
                                   serialized_event       TEXT                     NOT NULL,
                                   publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
                                   completion_date        TIMESTAMP WITH TIME ZONE,
                                   status                 TEXT,
                                   completion_attempts    INT,
                                   last_resubmission_date TIMESTAMP WITH TIME ZONE,
                                   PRIMARY KEY (id)
);

CREATE INDEX event_publication_serialized_event_hash_idx ON event_publication USING hash (serialized_event);
CREATE INDEX event_publication_by_completion_date_idx ON event_publication (completion_date);
