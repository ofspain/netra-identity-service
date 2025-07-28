-- Table: identity_role (many-to-many between identity and role)
CREATE TABLE identity_role_template (
  identity_id BIGINT NOT NULL,
  role_template_id BIGINT NOT NULL,
  PRIMARY KEY (identity_id, role_template_id),
  FOREIGN KEY (identity_id) REFERENCES identities(id),
  FOREIGN KEY (role_template_id) REFERENCES role_templates(id)
);