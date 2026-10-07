-- Weekly opening hours of the practitioner (docs/specification.md, 4.1).
CREATE TABLE opening_hours (
                               id          BIGINT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                               day_of_week SMALLINT NOT NULL,
                               start_time  TIME     NOT NULL,
                               end_time    TIME     NOT NULL,

                               CONSTRAINT opening_hours_day_ck CHECK (day_of_week BETWEEN 1 AND 7),
                               CONSTRAINT opening_hours_range_ck CHECK (start_time < end_time)
);

-- Monday to Thursday, 14:00 to 18:00 (ISO day numbers: 1 = Monday).
INSERT INTO opening_hours (day_of_week, start_time, end_time) VALUES
                                                                  (1, '14:00', '18:00'),
                                                                  (2, '14:00', '18:00'),
                                                                  (3, '14:00', '18:00'),
                                                                  (4, '14:00', '18:00');

-- Whole days off: holidays, illness (docs/specification.md, 4.3).
CREATE TABLE closure (
                         id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         first_day  DATE         NOT NULL,
                         last_day   DATE         NOT NULL,
                         reason     VARCHAR(200),
                         created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

                         CONSTRAINT closure_range_ck CHECK (first_day <= last_day)
);
