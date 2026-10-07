-- Clients of the practitioner, as known by the booking module (docs/specification.md, 5.2 and 6).
CREATE TABLE client (
                        id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        email      VARCHAR(254) NOT NULL,
                        first_name VARCHAR(80)  NOT NULL,
                        last_name  VARCHAR(80)  NOT NULL,
                        birth_date DATE         NOT NULL,
                        phone      VARCHAR(20)  NOT NULL,
                        city       VARCHAR(100),
                        trusted    BOOLEAN      NOT NULL DEFAULT FALSE,
                        blocked    BOOLEAN      NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
                        updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

                        CONSTRAINT client_email_uk UNIQUE (email),
                        CONSTRAINT client_email_lowercase_ck CHECK (email = lower(email))
);

-- Appointments. A requested or confirmed one holds its slot and the break that follows (4.2, 5.5).
CREATE TABLE appointment (
                             id             BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                             client_id      BIGINT       NOT NULL REFERENCES client (id),
                             offering_slug  VARCHAR(80)  NOT NULL REFERENCES offering (slug),
                             offering_name  VARCHAR(120) NOT NULL,
                             price_cents    INTEGER      NOT NULL,
                             mode           VARCHAR(20)  NOT NULL,
                             starts_at      TIMESTAMPTZ  NOT NULL,
                             ends_at        TIMESTAMPTZ  NOT NULL,
                             occupied_until TIMESTAMPTZ  NOT NULL,
                             status         VARCHAR(20)  NOT NULL,
                             reason         TEXT         NOT NULL,
                             address        VARCHAR(300),
                             messenger_name VARCHAR(100),
                             consented_at   TIMESTAMPTZ  NOT NULL,
                             created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

                             CONSTRAINT appointment_mode_ck CHECK (mode IN ('IN_PERSON', 'CLIENT_HOME', 'VIDEO', 'PHONE')),
                             CONSTRAINT appointment_status_ck CHECK (status IN
                                                                     ('REQUESTED', 'CONFIRMED', 'DECLINED', 'EXPIRED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')),
                             CONSTRAINT appointment_times_ck CHECK (starts_at < ends_at AND ends_at <= occupied_until),
                             CONSTRAINT appointment_address_ck CHECK (mode <> 'CLIENT_HOME' OR address IS NOT NULL),
                             CONSTRAINT appointment_messenger_ck CHECK (mode <> 'VIDEO' OR messenger_name IS NOT NULL),
    -- Two sessions holding a slot never overlap, breaks included.
                             CONSTRAINT appointment_no_overlap EXCLUDE USING gist (tstzrange(starts_at, occupied_until) WITH &&)
        WHERE (status IN ('REQUESTED', 'CONFIRMED'))
);

CREATE INDEX appointment_client_idx ON appointment (client_id);
CREATE INDEX appointment_starts_at_idx ON appointment (starts_at);

-- Portrait sent with a clairvoyance request, deleted some days after the session (5.2, 9).
CREATE TABLE appointment_photo (
                                   appointment_id BIGINT      PRIMARY KEY REFERENCES appointment (id) ON DELETE CASCADE,
                                   content_type   VARCHAR(50) NOT NULL,
                                   data           BYTEA       NOT NULL,
                                   uploaded_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
