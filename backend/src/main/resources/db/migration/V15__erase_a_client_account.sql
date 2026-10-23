-- The right to erasure (docs/specification.md, 9): a client erases her account herself.
-- Her sessions and their bills stay, because the law asks for them, but nothing identifies her any more.
ALTER TABLE client ADD COLUMN erased_at TIMESTAMPTZ;

-- An erased client keeps no email, no birth date and no city. Her phone number stays only while she
-- is blocked, so that the block list keeps working (6).
ALTER TABLE client ADD CONSTRAINT client_erased_ck
    CHECK (erased_at IS NULL OR (email IS NULL AND birth_date IS NULL AND city IS NULL AND NOT trusted));
