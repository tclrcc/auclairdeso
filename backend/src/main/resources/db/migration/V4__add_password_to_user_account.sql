-- Second factor for the practitioner and the administrator.
-- Clients never have a password: they only log in with a magic link.
ALTER TABLE user_account ADD COLUMN password_hash VARCHAR(255);

ALTER TABLE user_account
    ADD CONSTRAINT user_account_password_staff_only_ck
        CHECK (password_hash IS NULL OR role <> 'CLIENT');
