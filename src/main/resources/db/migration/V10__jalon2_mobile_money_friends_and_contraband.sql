-- ============================================================================
-- CITY LIFE - ABIDJAN | MIGRATION V10 (Jalon 2 — Lagune Money, Amis & RP PEGI 16+)
-- 100 % rétro-compatible (IF NOT EXISTS / ON CONFLICT DO NOTHING)
-- ============================================================================

-- 1. Table des amitiés entre joueurs (demandes, acceptations, liste d'amis)
CREATE TABLE IF NOT EXISTS friendships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    addressee_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_friendships_not_self CHECK (requester_player_id <> addressee_player_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_friendships_pair
    ON friendships (
        LEAST(requester_player_id, addressee_player_id),
        GREATEST(requester_player_id, addressee_player_id)
    );

CREATE INDEX IF NOT EXISTS idx_friendships_requester ON friendships (requester_player_id, status);
CREATE INDEX IF NOT EXISTS idx_friendships_addressee ON friendships (addressee_player_id, status);

-- 2. Cooldown serveur strict anti-triche dédié aux livraisons fictives à risque (Contrebande PEGI 16+)
ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS last_contraband_payout_at TIMESTAMPTZ;

-- 3. Présence récente pour le statut en ligne de la liste d'amis
ALTER TABLE players
    ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMPTZ;
