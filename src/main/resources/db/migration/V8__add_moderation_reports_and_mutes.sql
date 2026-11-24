-- ============================================================================
-- CITY LIFE - ABIDJAN | MIGRATION V8 (Phase 16C — Modération, Signalements & Mute Joueurs)
-- 100 % rétro-compatible (IF NOT EXISTS / valeurs par défaut)
-- ============================================================================

ALTER TABLE players
    ADD COLUMN IF NOT EXISTS muted_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS mute_reason VARCHAR(255);

CREATE TABLE IF NOT EXISTS player_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_player_id UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    target_username VARCHAR(80) NOT NULL,
    reason_code VARCHAR(30) NOT NULL CHECK (reason_code IN ('INSULTES', 'SPAM', 'TRICHE', 'AUTRE')),
    message_excerpt VARCHAR(400) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'DISMISSED')),
    resolution_note VARCHAR(300),
    resolved_by_account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_player_reports_status ON player_reports(status);
CREATE INDEX IF NOT EXISTS idx_player_reports_created_at ON player_reports(created_at);
