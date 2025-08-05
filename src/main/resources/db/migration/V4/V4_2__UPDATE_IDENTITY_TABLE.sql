ALTER TABLE identities
    ALTER COLUMN domain_code SET NOT NULL;


ALTER TABLE identities
    ADD COLUMN domain_type VARCHAR(60) NOT NULL,
    ADD COLUMN user_id BIGINT NOT NULL,
    ADD CONSTRAINT unique_domain_code_user_id UNIQUE (domain_code, user_id),
    ADD CONSTRAINT unique_domain_type_user_id UNIQUE (domain_type, user_id);
