-- Client files (docs/specification.md, 6 and 8): late cancellations that lead to a block, private notes.

-- Late cancellations since the client was last unblocked: unblocking gives her a fresh start.
ALTER TABLE client ADD COLUMN late_cancellations INTEGER NOT NULL DEFAULT 0;
UPDATE client c
SET late_cancellations = (SELECT count(*) FROM appointment a WHERE a.client_id = c.id AND a.late_cancellation);
ALTER TABLE client ADD CONSTRAINT client_late_cancellations_ck CHECK (late_cancellations >= 0);

-- Private notes of the practitioner about a client, never shown to the client (8 and 9).
CREATE TABLE client_note (
                             id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             client_id  BIGINT      NOT NULL REFERENCES client (id) ON DELETE CASCADE,
                             text       TEXT        NOT NULL,
                             created_at TIMESTAMPTZ NOT NULL,

                             CONSTRAINT client_note_text_ck CHECK (length(text) BETWEEN 1 AND 4000)
);

CREATE INDEX client_note_client_idx ON client_note (client_id, created_at DESC);
