-- ============================================================================
-- CITY LIFE - ABIDJAN | MIGRATION V6 (Phase 16A — Besoins RP, Consommables & Hôpital CHU)
-- 100 % rétro-compatible (ADD COLUMN IF NOT EXISTS + ON CONFLICT DO NOTHING)
-- ============================================================================

ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS hospital_visits INTEGER NOT NULL DEFAULT 0;

INSERT INTO items (item_code, name, description, category, base_price, weight_grams, is_stackable, max_stack, is_tradable, effects)
VALUES
    (
        'FOOD_ALLOCO',
        'Assiette d''Alloco',
        'Banane plantain frite dorée accompagnée de sauce pimentée. Restaure la faim et l''énergie.',
        'FOOD',
        800,
        300,
        TRUE,
        10,
        TRUE,
        '{"hunger_restore": 35, "stamina_restore": 15}'::jsonb
    ),
    (
        'DRINK_BISSAP',
        'Jus de Bissap Frais',
        'Boisson fraîche aux fleurs d''hibiscus préparée à Abidjan. Hydrate et redonne de l''énergie.',
        'DRINK',
        500,
        400,
        TRUE,
        15,
        TRUE,
        '{"thirst_restore": 45, "stamina_restore": 20}'::jsonb
    ),
    (
        'MED_KIT_CHU',
        'Trousse de Soins Pharmacie',
        'Trousse de premiers secours vendue en pharmacie. Restaure la santé.',
        'MISC',
        2500,
        450,
        TRUE,
        5,
        TRUE,
        '{"health_restore": 50, "stamina_restore": 15}'::jsonb
    )
ON CONFLICT (item_code) DO NOTHING;
