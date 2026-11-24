-- ============================================================================
-- CITY LIFE - ABIDJAN | MIGRATION V7 (Phase 16B — Armurerie Arcade PEGI 16+, Police & Factions RP)
-- 100 % rétro-compatible (IF NOT EXISTS / valeurs par défaut / ON CONFLICT DO NOTHING)
-- ============================================================================

ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS has_weapon_license BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS wanted_level INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_fines_paid BIGINT NOT NULL DEFAULT 0;

-- Catalogue Armurerie Lagune (PEGI 16+ arcade sans gore, tarifs validés en FCFA)
INSERT INTO items (item_code, name, description, category, base_price, weight_grams, is_stackable, max_stack, is_tradable, effects)
VALUES
    (
        'LICENSE_WEAPON_PERMIT',
        'Permis de Port d''Arme',
        'Autorisation officielle de l''Armurerie Lagune requise pour le Taser, le Pistolet 9mm et le Fusil Tactique.',
        'DOCUMENT',
        15000,
        20,
        FALSE,
        1,
        TRUE,
        '{"grants_weapon_license": true}'::jsonb
    ),
    (
        'WEAPON_BATON',
        'Matraque Télescopique',
        'Arme de mêlée arcade non-létale. Aucune licence requise.',
        'TOOL',
        4000,
        650,
        FALSE,
        1,
        TRUE,
        '{"weapon_id": "WEAPON_BATON", "requires_license": false, "damage": 14, "range": 2.4}'::jsonb
    ),
    (
        'WEAPON_BAT',
        'Batte Sport Abidjan',
        'Arme de mêlée arcade en aluminium léger. Aucune licence requise.',
        'TOOL',
        5000,
        900,
        FALSE,
        1,
        TRUE,
        '{"weapon_id": "WEAPON_BAT", "requires_license": false, "damage": 18, "range": 2.6}'::jsonb
    ),
    (
        'WEAPON_TASER',
        'Taser Électrique Non-Létal',
        'Neutralise brièvement une cible hors Safe Zone sans blessure grave. Permis requis.',
        'TOOL',
        12000,
        450,
        FALSE,
        1,
        TRUE,
        '{"weapon_id": "WEAPON_TASER", "requires_license": true, "damage": 12, "stun_seconds": 3.0, "range": 14.0}'::jsonb
    ),
    (
        'WEAPON_PISTOL_9MM',
        'Pistolet 9mm Arcade',
        'Pistolet semi-automatique arcade stylisé (effets propres sans gore). Permis requis.',
        'TOOL',
        25000,
        850,
        FALSE,
        1,
        TRUE,
        '{"weapon_id": "WEAPON_PISTOL_9MM", "requires_license": true, "damage": 20, "range": 38.0, "ammo_code": "AMMO_9MM_ARCADE"}'::jsonb
    ),
    (
        'WEAPON_TACTICAL_RIFLE',
        'Fusil Tactique Arcade',
        'Carabine tactique arcade précise à moyenne distance (effets propres sans gore). Permis requis.',
        'TOOL',
        45000,
        2400,
        FALSE,
        1,
        TRUE,
        '{"weapon_id": "WEAPON_TACTICAL_RIFLE", "requires_license": true, "damage": 26, "range": 55.0, "ammo_code": "AMMO_RIFLE_ARCADE"}'::jsonb
    ),
    (
        'AMMO_9MM_ARCADE',
        'Munitions 9mm Arcade (x24)',
        'Boîte de 24 cartouches pour Pistolet 9mm Arcade.',
        'MISC',
        2000,
        250,
        TRUE,
        20,
        TRUE,
        '{"ammo_for": "WEAPON_PISTOL_9MM", "rounds": 24}'::jsonb
    ),
    (
        'AMMO_RIFLE_ARCADE',
        'Munitions Tactiques Arcade (x30)',
        'Boîte de 30 cartouches pour Fusil Tactique Arcade.',
        'MISC',
        2000,
        350,
        TRUE,
        20,
        TRUE,
        '{"ammo_for": "WEAPON_TACTICAL_RIFLE", "rounds": 30}'::jsonb
    )
ON CONFLICT (item_code) DO NOTHING;

-- Factions / Gangs RP d'Abidjan
CREATE TABLE IF NOT EXISTS factions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(16) NOT NULL UNIQUE,
    tag VARCHAR(8) NOT NULL UNIQUE,
    name VARCHAR(80) NOT NULL UNIQUE,
    color_hex VARCHAR(16) NOT NULL DEFAULT '#F5C518',
    leader_character_id UUID REFERENCES characters(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS faction_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    faction_id UUID NOT NULL REFERENCES factions(id) ON DELETE CASCADE,
    character_id UUID NOT NULL UNIQUE REFERENCES characters(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('LEADER', 'OFFICER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_faction_members_faction_id ON faction_members(faction_id);

CREATE TABLE IF NOT EXISTS faction_territories (
    zone_code VARCHAR(40) PRIMARY KEY,
    zone_name VARCHAR(80) NOT NULL,
    owner_faction_id UUID REFERENCES factions(id) ON DELETE SET NULL,
    captured_at TIMESTAMPTZ
);

INSERT INTO faction_territories (zone_code, zone_name, owner_faction_id, captured_at)
VALUES
    ('ZONE_PORT_VRIDI', 'Port de Vridi', NULL, NULL),
    ('ZONE_INDUS_YOPOUGON', 'Zone Industrielle Yopougon', NULL, NULL)
ON CONFLICT (zone_code) DO NOTHING;
