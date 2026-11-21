-- ============================================================================
-- CITY LIFE - ABIDJAN | PHASE 11 : TUTORIEL & ONBOARDING SAUVEGARDÉ SERVEUR
-- Migration 100 % rétro-compatible sur la table players
-- ============================================================================

ALTER TABLE players
    ADD COLUMN IF NOT EXISTS tutorial_completed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS tutorial_step INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS tutorial_completed_at TIMESTAMPTZ;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_players_tutorial_step_non_negative'
    ) THEN
        ALTER TABLE players
            ADD CONSTRAINT chk_players_tutorial_step_non_negative
            CHECK (tutorial_step >= 0);
    END IF;
END $$;
