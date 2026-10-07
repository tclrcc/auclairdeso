-- Offering categories (docs/specification.md, 3.1): clairvoyance offerings require a photo of the client.
ALTER TABLE offering ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'CLAIRVOYANCE';
ALTER TABLE offering ADD CONSTRAINT offering_category_ck
    CHECK (category IN ('CLAIRVOYANCE', 'MAGNETISM', 'ENERGY_REBALANCING'));
