-- Accounts of clients, the practitioner and the administrator.
CREATE TABLE user_account (
                              id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              email         VARCHAR(254) NOT NULL,
                              role          VARCHAR(20)  NOT NULL DEFAULT 'CLIENT',
                              created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                              last_login_at TIMESTAMPTZ,

                              CONSTRAINT user_account_email_uk UNIQUE (email),
                              CONSTRAINT user_account_email_lowercase_ck CHECK (email = lower(email)),
                              CONSTRAINT user_account_role_ck CHECK (role IN ('CLIENT', 'PRACTITIONER', 'ADMIN'))
);
