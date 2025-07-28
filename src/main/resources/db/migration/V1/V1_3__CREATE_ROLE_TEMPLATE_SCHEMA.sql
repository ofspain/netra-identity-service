CREATE TABLE role_templates (
  id BIGSERIAL PRIMARY KEY,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  name VARCHAR(100),
  description TEXT
);

CREATE INDEX idx_role_templates_name ON role_templates(name);

