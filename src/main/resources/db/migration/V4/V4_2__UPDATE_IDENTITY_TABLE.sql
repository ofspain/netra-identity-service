ALTER TABLE identities
    ALTER COLUMN domain_code SET NOT NULL;


ALTER TABLE identities
    ADD COLUMN domain_type VARCHAR(60) NOT NULL,
    ADD COLUMN identity_uuid VARCHAR(100) NOT NULL UNIQUE;



