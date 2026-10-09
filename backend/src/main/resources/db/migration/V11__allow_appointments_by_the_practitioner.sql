-- Appointments taken by the practitioner herself: phone, Messenger, urgent sessions (docs/specification.md, 4.3 and 8).
ALTER TABLE client ALTER COLUMN email DROP NOT NULL;
ALTER TABLE client ALTER COLUMN birth_date DROP NOT NULL;

ALTER TABLE appointment ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'ONLINE';
ALTER TABLE appointment ADD CONSTRAINT appointment_source_ck CHECK (source IN ('ONLINE', 'PRACTITIONER'));
ALTER TABLE appointment ALTER COLUMN reason DROP NOT NULL;
ALTER TABLE appointment ALTER COLUMN consented_at DROP NOT NULL;

-- An online request always carries the client's consent and her reason.
ALTER TABLE appointment ADD CONSTRAINT appointment_online_consent_ck
    CHECK (source <> 'ONLINE' OR (consented_at IS NOT NULL AND reason IS NOT NULL));
