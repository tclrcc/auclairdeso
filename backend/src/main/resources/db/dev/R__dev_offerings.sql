-- Development data only, loaded with the "dev" profile. Fictional content.
INSERT INTO offering (slug, name, description, duration_minutes, price_cents,
                      payment_policy, deposit_cents, display_order, active)
VALUES
    ('guidance-30-min', 'Guidance — 30 min',
     'Une séance courte pour faire le point sur une question précise.',
     30, 4500, 'FULL_ONLINE', NULL, 10, TRUE),
    ('guidance-1-h', 'Guidance — 1 h',
     'Une séance complète pour explorer plusieurs aspects de votre situation.',
     60, 8000, 'DEPOSIT_ONLINE', 3000, 20, TRUE),
    ('contact-defunt-1-h', 'Contact avec un défunt — 1 h',
     'Une séance consacrée à un proche disparu.',
     60, 9000, 'ON_SITE', NULL, 30, TRUE),
    ('ancienne-seance', 'Ancienne séance',
     'Prestation désactivée : elle ne doit jamais apparaître.',
     45, 6000, 'FULL_ONLINE', NULL, 40, FALSE)
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
                   ('ancienne-seance', 'PHONE')
) AS m (slug, mode) ON m.slug = o.slug
    ON CONFLICT DO NOTHING;
