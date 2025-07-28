CREATE TABLE roles (
     id BIGSERIAL PRIMARY KEY,
     created_at TIMESTAMP,
     updated_at TIMESTAMP,
     name VARCHAR(100),
     description TEXT
);

CREATE INDEX idx_roles_name ON roles(name);
