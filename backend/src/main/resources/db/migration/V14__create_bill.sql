-- Bills of completed sessions and their payment (docs/specification.md, 3.5 and 10).
-- A bill is the "note" every service of 25 euros or more requires (arrêté 83-50/A du 3 octobre 1983):
-- its content is copied at issue time and never changes, only its payment does.

-- The last number given each year: numbers follow each other without gaps, "2026-0001", "2026-0002"...
CREATE TABLE bill_number (
                             year        INTEGER PRIMARY KEY,
                             last_number INTEGER NOT NULL CHECK (last_number > 0)
);

CREATE TABLE bill (
                      id                   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                      number               VARCHAR(20)  NOT NULL,
                      appointment_id       BIGINT       NOT NULL REFERENCES appointment (id),
                      issued_on            DATE         NOT NULL,
                      practitioner_name    VARCHAR(120) NOT NULL,
                      practitioner_address VARCHAR(300) NOT NULL,
                      siret                VARCHAR(20)  NOT NULL,
                      client_name          VARCHAR(170) NOT NULL,
                      client_email         VARCHAR(254),
                      service_start        TIMESTAMPTZ  NOT NULL,
                      service_place        VARCHAR(400) NOT NULL,
                      description          VARCHAR(200) NOT NULL,
                      amount_cents         INTEGER      NOT NULL,
                      payment_method       VARCHAR(20),
                      paid_on              DATE,
                      created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

                      CONSTRAINT bill_number_uk UNIQUE (number),
                      CONSTRAINT bill_appointment_uk UNIQUE (appointment_id),
                      CONSTRAINT bill_amount_ck CHECK (amount_cents > 0),
                      CONSTRAINT bill_payment_method_ck CHECK (payment_method IN ('TRANSFER', 'CASH', 'MOBILE')),
    -- Paid means both: how, and when.
                      CONSTRAINT bill_payment_ck CHECK ((payment_method IS NULL) = (paid_on IS NULL))
);

-- The receipt book lists the payments of a year, in date order.
CREATE INDEX bill_paid_on_idx ON bill (paid_on, number);
