-- ============================================================================
-- CITY LIFE - ABIDJAN | LOT 4 : CATALOGUE INITIAL (PROTOTYPE V0.1) & VÉRIFICATION
-- Base : city_life_abidjan
-- ============================================================================

-- 1. Objets de base (ITEMS)
INSERT INTO items (item_code, name, description, category, base_price, weight_grams, is_stackable, max_stack, is_tradable, effects)
VALUES
    ('PHONE_SMART_01', 'Smartphone Abidjanais', 'Téléphone virtuel permettant d''accéder aux contacts, à la banque et aux missions.', 'PHONE', 45000, 180, FALSE, 1, TRUE, '{}'::jsonb),
    ('DOC_ID_CARD', 'Carte d''Identité Cedeao', 'Pièce d''identité officielle du citoyen d''Abidjan.', 'DOCUMENT', 5000, 10, FALSE, 1, FALSE, '{}'::jsonb),
    ('FOOD_GARBA', 'Plat de Garba', 'Attiéké chaud avec thon frit et piment frais. Restaure la faim.', 'FOOD', 1000, 350, TRUE, 10, TRUE, '{"hunger_restore": 45}'::jsonb),
    ('DRINK_WATER_50CL', 'Sachet / Bouteille d''Eau Fraîche', 'Eau minérale fraîche pour s''hydrater sous le soleil d''Abidjan.', 'DRINK', 500, 500, TRUE, 20, TRUE, '{"thirst_restore": 40}'::jsonb)
ON CONFLICT (item_code) DO NOTHING;

-- 2. Véhicules de base (VEHICLES) - Inclut la 1ère voiture pour la V0.1
INSERT INTO vehicles (vehicle_code, name, brand, vehicle_type, base_price, max_speed_kmh, seats, fuel_capacity, trunk_capacity_grams)
VALUES
    ('CAR_SEDAN_V01', 'Berline Citadine Ivoire', 'IvoireMotors', 'CAR', 2500000, 160, 4, 55, 60000),
    ('TAXI_COMPTEUR_01', 'Taxi Compteur Orange', 'IvoireMotors', 'TAXI_COMPTEUR', 3200000, 150, 4, 60, 50000),
    ('BUS_GBAKA_18P', 'Minibus Gbaka 18 Places', 'LaguneAuto', 'BUS_GBAKA', 6500000, 115, 18, 85, 120000)
ON CONFLICT (vehicle_code) DO NOTHING;

-- 3. Bâtiments / Propriétés de base (PROPERTIES) - Inclut le 1er bâtiment accessible V0.1
INSERT INTO properties (property_code, name, property_type, district, purchase_price, rent_price, storage_capacity_grams, entrance_x, entrance_y, entrance_z, is_unique)
VALUES
    ('PROP_PLATEAU_HALL_01', 'Résidence Lagune - Plateau (Bâtiment V0.1)', 'APARTMENT', 'PLATEAU', 15000000, 150000, 250000, 12.5, 0.0, -8.0, FALSE),
    ('PROP_MARCORY_STUDIO_01', 'Studio Résidentiel Zone 4', 'STUDIO', 'MARCORY', 4500000, 60000, 100000, -45.0, 0.0, 22.0, FALSE)
ON CONFLICT (property_code) DO NOTHING;

-- 4. Première mission officielle de la V0.1 (MISSIONS)
INSERT INTO missions (mission_code, title, description, district, min_level, reward_cash, reward_xp, is_repeatable, objectives)
VALUES
    (
        'MISSION_V01_WELCOME',
        'Premiers pas au Plateau',
        'Visite le bâtiment accessible du Plateau, récupère ton véhicule de départ et effectue ton premier trajet en ville.',
        'PLATEAU',
        1,
        15000,
        100,
        FALSE,
        '[
            {"step": 1, "code": "ENTER_BUILDING", "label": "Entrer dans la Résidence Lagune au Plateau"},
            {"step": 2, "code": "DRIVE_CAR", "label": "Monter dans la Berline Citadine et conduire jusqu''au point de contrôle"}
        ]'::jsonb
    )
ON CONFLICT (mission_code) DO NOTHING;

-- 5. Vérification du catalogue inséré et du nombre total de contraintes de sécurité
SELECT 
    (SELECT COUNT(*) FROM items) AS nb_items,
    (SELECT COUNT(*) FROM vehicles) AS nb_vehicles,
    (SELECT COUNT(*) FROM properties) AS nb_properties,
    (SELECT COUNT(*) FROM missions) AS nb_missions,
    (SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_schema = 'public') AS nb_contraintes_securite;