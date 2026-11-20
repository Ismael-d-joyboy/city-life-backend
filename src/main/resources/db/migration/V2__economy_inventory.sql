CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID NOT NULL UNIQUE REFERENCES characters(id) ON DELETE CASCADE,
    cash_balance BIGINT NOT NULL DEFAULT 25000 CHECK (cash_balance >= 0),
    bank_balance BIGINT NOT NULL DEFAULT 50000 CHECK (bank_balance >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'XOF',
    is_frozen BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER trg_wallets_updated_at BEFORE UPDATE ON wallets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TABLE transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    source_wallet_id UUID REFERENCES wallets(id) ON DELETE RESTRICT,
    target_wallet_id UUID REFERENCES wallets(id) ON DELETE RESTRICT,
    amount BIGINT NOT NULL CHECK (amount > 0),
    transaction_type VARCHAR(40) NOT NULL CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER', 'PURCHASE', 'SALE', 'MISSION_REWARD', 'SALARY', 'FINE', 'SYSTEM_GRANT')),
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')),
    description VARCHAR(255) NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_wallets_not_both_null CHECK (source_wallet_id IS NOT NULL OR target_wallet_id IS NOT NULL)
);
CREATE INDEX idx_transactions_source_wallet ON transactions(source_wallet_id);
CREATE INDEX idx_transactions_target_wallet ON transactions(target_wallet_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

CREATE TABLE items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(40) NOT NULL CHECK (category IN ('FOOD', 'DRINK', 'TOOL', 'PHONE', 'KEY', 'DOCUMENT', 'CLOTHING', 'MISC')),
    base_price BIGINT NOT NULL DEFAULT 0 CHECK (base_price >= 0),
    weight_grams INTEGER NOT NULL DEFAULT 100 CHECK (weight_grams >= 0),
    is_stackable BOOLEAN NOT NULL DEFAULT TRUE,
    max_stack INTEGER NOT NULL DEFAULT 50 CHECK (max_stack >= 1),
    is_tradable BOOLEAN NOT NULL DEFAULT TRUE,
    effects JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inventories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    character_id UUID NOT NULL REFERENCES characters(id) ON DELETE CASCADE,
    item_id UUID NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    slot_index INTEGER CHECK (slot_index >= 0),
    custom_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_character_item UNIQUE (character_id, item_id)
);
CREATE INDEX idx_inventories_character_id ON inventories(character_id);
CREATE TRIGGER trg_inventories_updated_at BEFORE UPDATE ON inventories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();