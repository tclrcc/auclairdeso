-- Development data only, loaded with the "dev" profile.
-- The four offerings of the catalogue agreed with the practitioner that have a time slot
-- (docs/specification.md, 3.2). The targeted question (15 €, no slot) is not here: it needs its own handling.
-- The model has one fixed duration per offering, so a variable one is declared at its upper bound.
-- No online payment: every offering is ON_SITE, without deposit (docs/specification.md, 3.5).
-- Repeatable migration: it runs again whenever this file changes, so it must converge to the same
-- state from an empty database and from one that already holds the previous seed.
INSERT INTO offering (slug, name, description, category, duration_minutes, buffer_minutes, price_cents,
                      payment_policy, deposit_cents, display_order, active)
VALUES
    ('guidance-1-h', 'Guidance — 1 h',
     'Une heure d''échange pour poser vos questions et recevoir un éclairage sur votre situation. La séance se déroule à votre rythme.',
     'CLAIRVOYANCE', 60, 15, 8000, 'ON_SITE', NULL, 10, TRUE),
    ('guidance-30-min', 'Guidance — 30 min',
     'Un format court, pour faire le point sur une question précise ou une période de votre vie.',
     'CLAIRVOYANCE', 30, 30, 5000, 'ON_SITE', NULL, 20, TRUE),
    ('magnetisme', 'Magnétisme',
     'Séance de magnétisme, pratique de bien-être qui se déroule en douceur. Durée de 30 à 45 minutes selon la séance. Elle ne remplace pas un suivi médical.',
     'MAGNETISM', 45, 15, 6000, 'ON_SITE', NULL, 30, TRUE),
    ('reequilibrage-energetique', 'Rééquilibrage énergétique',
     'Séance de bien-être pour se recentrer et retrouver du calme. Durée de 45 minutes à 1 heure selon le besoin. Trois séances espacées d''un mois ou plus sont conseillées, sans quantité imposée. Elle ne remplace pas un suivi médical.',
     'ENERGY_REBALANCING', 60, 15, 7000, 'ON_SITE', NULL, 40, TRUE)
    ON CONFLICT (slug) DO UPDATE
        SET name             = EXCLUDED.name,
            description      = EXCLUDED.description,
            category         = EXCLUDED.category,
            duration_minutes = EXCLUDED.duration_minutes,
            buffer_minutes   = EXCLUDED.buffer_minutes,
            price_cents      = EXCLUDED.price_cents,
            payment_policy   = EXCLUDED.payment_policy,
            deposit_cents    = EXCLUDED.deposit_cents,
            display_order    = EXCLUDED.display_order,
            active           = EXCLUDED.active,
            updated_at       = now();

-- Anything else left by a previous seed (a deceased-contact offering, an old inactive one...) goes away.
-- An offering that an appointment still points to cannot be deleted: it is hidden instead.
DELETE FROM offering o
WHERE o.slug NOT IN ('guidance-1-h', 'guidance-30-min', 'magnetisme', 'reequilibrage-energetique')
  AND NOT EXISTS (SELECT 1 FROM appointment a WHERE a.offering_slug = o.slug);

UPDATE offering
SET active = FALSE
WHERE slug NOT IN ('guidance-1-h', 'guidance-30-min', 'magnetisme', 'reequilibrage-energetique');

DELETE FROM offering_mode
WHERE offering_id IN (SELECT id
                      FROM offering
                      WHERE slug IN ('guidance-1-h', 'guidance-30-min', 'magnetisme', 'reequilibrage-energetique'));

INSERT INTO offering_mode (offering_id, mode)
SELECT o.id, m.mode
FROM offering o
         JOIN (VALUES
                   ('guidance-1-h', 'PHONE'),
                   ('guidance-1-h', 'VIDEO'),
                   ('guidance-1-h', 'IN_PERSON'),
                   ('guidance-30-min', 'PHONE'),
                   ('guidance-30-min', 'VIDEO'),
                   ('magnetisme', 'IN_PERSON'),
                   ('magnetisme', 'PHONE'),
                   ('reequilibrage-energetique', 'IN_PERSON'),
                   ('reequilibrage-energetique', 'PHONE')
) AS m (slug, mode) ON m.slug = o.slug
    ON CONFLICT DO NOTHING;
