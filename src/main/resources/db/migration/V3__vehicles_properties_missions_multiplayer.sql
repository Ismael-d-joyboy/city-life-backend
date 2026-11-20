-- ============================================================================
-- CITY LIFE - ABIDJAN | LOT 3 : VÉHICULES, IMMOBILIER, MISSIONS & MULTIJOUEUR
-- Base : city_life_abidjan
-- ============================================================================

-- 1. Table VEHICLES (Catalogue officiel des véhicules)
CREATE TABLE vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    brand VARCHAR(60) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL
        CHECK (vehicle_type IN ('CAR', 'MOTO', 'BUS_GBAKA', 'TAXI_COMPTEUR', 'WOROWORO', 'TRUCK')),
    base_price BIGINT NOT NULL CHECK (base_price >= 0),
    max_speed_kmh INTEGER NOT NULL DEFAULT 120 CHECK (max_speed_kmh > 0),
    seats INTEGER NOT NULL DEFAULT 4 CHECK (seats >= 1),
    fuel_capacity INTEGER NOT NULL DEFAULT 60 CHECK (fuel_capacity > 0),
    trunk_capacity_grams INTEGER NOT NULL DEFAULT 50000 CHECK (trunk_capacity_grams >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Table PLAYER_VEHICLES (Véhicules possédés par un personnage)
CREATE TABLE player_vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    vehicle_id UUID NOT NULL REFERENCES vehicles(id) ON DELETE RESTRICT,
    license_plate VARCHAR(20) NOT NULL UNIQUE,
    color_hex VARCHAR(15) NOT NULL DEFAULT '#FFFFFF',
    fuel_level INTEGER NOT NULL DEFAULT 100 CHECK (fuel_level BETWEEN 0 AND 100),
    engine_health INTEGER NOT NULL DEFAULT 100 CHECK (engine_health BETWEEN 0 AND 100),
    body_health INTEGER NOT NULL DEFAULT 100 CHECK (body_health BETWEEN 0 AND 100),
    is_locked BOOLEAN NOT NULL DEFAULT TRUE,
    is_stored BOOLEAN NOT NULL DEFAULT TRUE,
    is_impounded BOOLEAN NOT NULL DEFAULT FALSE,
    district VARCHAR(50) NOT NULL DEFAULT 'PLATEAU',
    pos_x DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    pos_y DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    pos_z DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    rotation_y DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    customizations JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_player_vehicles_character_id ON player_vehicles(character_id);

CREATE TRIGGER trg_player_vehicles_updated_at
BEFORE UPDATE ON player_vehicles
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3. Table PROPERTIES (Catalogue immobilier : maisons, appartements, commerces)
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    property_type VARCHAR(30) NOT NULL
        CHECK (property_type IN ('APARTMENT', 'VILLA', 'STUDIO', 'SHOP', 'GARAGE', 'WAREHOUSE')),
    district VARCHAR(50) NOT NULL
        CHECK (district IN ('PLATEAU', 'COCODY', 'TREICHVILLE', 'MARCORY', 'YOPOUGON', 'ABOBO', 'PORT_BOUET', 'BINGERVILLE')),
    purchase_price BIGINT NOT NULL CHECK (purchase_price >= 0),
    rent_price BIGINT NOT NULL DEFAULT 0 CHECK (rent_price >= 0),
    storage_capacity_grams INTEGER NOT NULL DEFAULT 200000 CHECK (storage_capacity_grams >= 0),
    entrance_x DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    entrance_y DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    entrance_z DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    is_unique BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_properties_district ON properties(district);

-- 4. Table PLAYER_PROPERTIES (Maisons et commerces possédés ou loués)
CREATE TABLE player_properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    property_id UUID NOT NULL REFERENCES properties(id) ON DELETE RESTRICT,
    ownership_type VARCHAR(20) NOT NULL DEFAULT 'OWNED'
        CHECK (ownership_type IN ('OWNED', 'RENTED')),
    is_locked BOOLEAN NOT NULL DEFAULT TRUE,
    rent_due_at TIMESTAMPTZ,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_character_property UNIQUE (character_id, property_id)
);

CREATE INDEX idx_player_properties_character_id ON player_properties(character_id);

CREATE TRIGGER trg_player_properties_updated_at
BEFORE UPDATE ON player_properties
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 5. Table MISSIONS (Catalogue des missions et activités)
CREATE TABLE missions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mission_code VARCHAR(60) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    district VARCHAR(50) NOT NULL DEFAULT 'PLATEAU',
    min_level INTEGER NOT NULL DEFAULT 1 CHECK (min_level >= 1),
    reward_cash BIGINT NOT NULL DEFAULT 0 CHECK (reward_cash >= 0),
    reward_xp BIGINT NOT NULL DEFAULT 0 CHECK (reward_xp >= 0),
    is_repeatable BOOLEAN NOT NULL DEFAULT FALSE,
    objectives JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Table PLAYER_MISSIONS (Progression des missions par personnage)
CREATE TABLE player_missions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    mission_id UUID NOT NULL REFERENCES missions(id) ON DELETE RESTRICT,
    status VARCHAR(25) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED', 'ABANDONED')),
    progress_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_player_missions_character_id ON player_missions(character_id);
CREATE INDEX idx_player_missions_status ON player_missions(status);

CREATE TRIGGER trg_player_missions_updated_at
BEFORE UPDATE ON player_missions
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 7. Table GAME_SESSIONS (Suivi des connexions en ligne & salons Photon Fusion)
CREATE TABLE game_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    character_id UUID REFERENCES characters(id) ON DELETE SET NULL,
    photon_room_name VARCHAR(100),
    photon_region VARCHAR(30) DEFAULT 'eu',
    ip_address VARCHAR(45),
    device_model VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    connected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMPTZ
);

CREATE INDEX idx_game_sessions_player_id ON game_sessions(player_id);
CREATE INDEX idx_game_sessions_active ON game_sessions(is_active);

-- 8. Attribution de la propriété à citylife_app


-- 9. Droits par défaut pour les futures tables et séquences du schéma public


-- 10. Vérification finale des 18 tables complètes de city_life_abidjan
SELECT tablename AS table_creee, tableowner AS proprietaire
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY tablename;