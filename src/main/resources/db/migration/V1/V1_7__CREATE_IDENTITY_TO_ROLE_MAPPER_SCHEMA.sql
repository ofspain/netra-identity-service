CREATE TABLE identity_role (
    identity_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (identity_id, role_id),
    FOREIGN KEY (identity_id) REFERENCES identities(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);