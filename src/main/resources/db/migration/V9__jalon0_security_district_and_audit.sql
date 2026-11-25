-- ============================================================================
-- CITY LIFE - ABIDJAN | MIGRATION V9 (Jalon 0 — Sécurité, ADJAME & Conformité)
-- 100 % rétro-compatible (IF NOT EXISTS / DO $$ / valeurs par défaut)
-- ============================================================================

-- 1. Ajouter ADJAME aux contraintes CHECK de district sur characters & properties
DO $$
DECLARE
    con_name TEXT;
BEGIN
    FOR con_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'characters'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%district%'
    LOOP
        EXECUTE format('ALTER TABLE characters DROP CONSTRAINT %I', con_name);
    END LOOP;

    ALTER TABLE characters
        ADD CONSTRAINT chk_characters_district
        CHECK (district IN ('PLATEAU', 'COCODY', 'TREICHVILLE', 'MARCORY', 'ADJAME', 'YOPOUGON', 'ABOBO', 'PORT_BOUET', 'BINGERVILLE'));

    FOR con_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'properties'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%district%'
    LOOP
        EXECUTE format('ALTER TABLE properties DROP CONSTRAINT %I', con_name);
    END LOOP;

    ALTER TABLE properties
        ADD CONSTRAINT chk_properties_district
        CHECK (district IN ('PLATEAU', 'COCODY', 'TREICHVILLE', 'MARCORY', 'ADJAME', 'YOPOUGON', 'ABOBO', 'PORT_BOUET', 'BINGERVILLE'));
END $$;

-- 2. Aligner les coordonnées SQL du Studio Zone 4 avec la scène Unity (-3.2, 0.0, -190.0)
UPDATE properties
SET entrance_x = -3.2,
    entrance_y = 0.0,
    entrance_z = -190.0
WHERE property_code = 'PROP_MARCORY_STUDIO_01';

-- 3. Anti-triche métiers persisté en base (fermeture exploit MANAWA_BONUS_5 & survie restart Render)
ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS manawa_bag_streak INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_job_payout_at TIMESTAMPTZ;

-- 4. Mode Pacifique optionnel par joueur (conforme PEGI 16+ / IARC)
ALTER TABLE players
    ADD COLUMN IF NOT EXISTS peaceful_mode BOOLEAN NOT NULL DEFAULT FALSE;
