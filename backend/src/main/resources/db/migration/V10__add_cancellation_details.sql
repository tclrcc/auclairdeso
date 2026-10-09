-- Who cancelled, and whether a client cancelled too late (docs/specification.md, 6).
ALTER TABLE appointment ADD COLUMN cancelled_by VARCHAR(20);
ALTER TABLE appointment ADD COLUMN late_cancellation BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE appointment ADD CONSTRAINT appointment_cancelled_by_value_ck
    CHECK (cancelled_by IN ('CLIENT', 'PRACTITIONER'));
ALTER TABLE appointment ADD CONSTRAINT appointment_cancelled_by_status_ck
    CHECK ((status = 'CANCELLED') = (cancelled_by IS NOT NULL));
ALTER TABLE appointment ADD CONSTRAINT appointment_late_cancellation_ck
    CHECK (NOT late_cancellation OR cancelled_by = 'CLIENT');
