CREATE TABLE password_histories (
    id BIGSERIAL PRIMARY KEY,
    identity_id BIGINT NOT NULL,
    hashed_password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP,
    CONSTRAINT fk_password_identities FOREIGN KEY (identity_id) REFERENCES identities(id)
);

CREATE INDEX idx_identity_id ON password_histories(identity_id);