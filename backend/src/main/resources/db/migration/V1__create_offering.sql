-- Consultations offered by the practitioner.
CREATE TABLE offering (
                          id               BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                          slug             VARCHAR(80)  NOT NULL,
                          name             VARCHAR(120) NOT NULL,
                          description      TEXT         NOT NULL,
                          duration_minutes INTEGER      NOT NULL,
                          price_cents      INTEGER      NOT NULL,
                          payment_policy   VARCHAR(20)  NOT NULL,
                          deposit_cents    INTEGER,
                          display_order    INTEGER      NOT NULL DEFAULT 0,
                          active           BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

                          CONSTRAINT offering_slug_uk UNIQUE (slug),
                          CONSTRAINT offering_slug_format_ck CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT offering_duration_ck CHECK (duration_minutes BETWEEN 15 AND 240),
    CONSTRAINT offering_price_ck CHECK (price_cents >= 0),
    CONSTRAINT offering_payment_policy_ck
        CHECK (payment_policy IN ('FULL_ONLINE', 'DEPOSIT_ONLINE', 'ON_SITE')),
    CONSTRAINT offering_deposit_ck CHECK (
        (payment_policy = 'DEPOSIT_ONLINE' AND deposit_cents BETWEEN 1 AND price_cents)
        OR (payment_policy <> 'DEPOSIT_ONLINE' AND deposit_cents IS NULL)
    )
);

-- Consultation modes available for each offering; the client picks one when booking.
CREATE TABLE offering_mode (
                               offering_id BIGINT      NOT NULL REFERENCES offering (id) ON DELETE CASCADE,
                               mode        VARCHAR(20) NOT NULL,

                               CONSTRAINT offering_mode_pk PRIMARY KEY (offering_id, mode),
                               CONSTRAINT offering_mode_value_ck
                                   CHECK (mode IN ('IN_PERSON', 'VIDEO', 'PHONE', 'WRITTEN'))
);
