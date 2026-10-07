-- Development data only, loaded with the "dev" profile. Fictional content.
INSERT INTO offering (slug, name, description, category, duration_minutes, buffer_minutes, price_cents,
                      payment_policy, deposit_cents, display_order, active)
VALUES
    ('guidance-30-min', 'Guidance — 30 min',
     'Une séance courte pour faire le point sur une question précise.',
     'CLAIRVOYANCE', 30, 30, 4500, 'FULL_ONLINE', NULL, 10, TRUE),
    ('guidance-1-h', 'Guidance — 1 h',
     'Une séance complète pour explorer plusieurs aspects de votre situation.',
     'CLAIRVOYANCE', 60, 15, 8000, 'DEPOSIT_ONLINE', 3000, 20, TRUE),
    ('contact-defunt-1-h', 'Contact avec un défunt — 1 h',
     'Une séance consacrée à un proche disparu.',
     'CLAIRVOYANCE', 60, 15, 9000, 'ON_SITE', NULL, 30, TRUE),
    ('magnetisme-30-min', 'Magnétisme — 30 min',
     'Une séance de magnétisme, en complément d''un suivi médical.',
     'MAGNETISM', 30, 30, 4000, 'ON_SITE', NULL, 35, TRUE),
    ('ancienne-seance', 'Ancienne séance',
     'Prestation désactivée : elle ne doit jamais apparaître.',
     'CLAIRVOYANCE', 45, 15, 6000, 'FULL_ONLINE', NULL, 40, FALSE)
    ON CONFLICT (slug) DO NOTHING;

INSERT INTO offering_mode (offering_id, mode)
SELECT o.id, m.mode
FROM offering o
         JOIN (VALUES
                   ('guidance-30-min', 'PHONE'),
                   ('guidance-30-min', 'VIDEO'),
                   ('guidance-1-h', 'PHONE'),
                   ('guidance-1-h', 'VIDEO'),
                   ('guidance-1-h', 'IN_PERSON'),
                   ('contact-defunt-1-h', 'IN_PERSON'),
                   ('magnetisme-30-min', 'IN_PERSON'),
                   ('magnetisme-30-min', 'PHONE'),
                   ('ancienne-seance', 'PHONE')
) AS m (slug, mode) ON m.slug = o.slug
    ON CONFLICT DO NOTHING;
