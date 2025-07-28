CREATE INDEX idx_identities_filtering
    ON identities (created_at, locked, disabled);