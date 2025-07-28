CREATE TABLE identities (
   id BIGSERIAL PRIMARY KEY,
   created_at TIMESTAMP,
   updated_at TIMESTAMP,
   username VARCHAR(100),
   password VARCHAR(255),
   disabled BOOLEAN,
   domain_code VARCHAR(50),
   password_last_changed TIMESTAMP,
   locked BOOLEAN,
   CONSTRAINT uq_username UNIQUE (username)
);

CREATE INDEX idx_identities_domain_code ON identities(domain_code);


-- -- Direct roles
-- SELECT r.*
-- FROM identity_role ir
--          JOIN roles r ON ir.role_id = r.id
-- WHERE ir.identity_id = :identityId
--
-- UNION
--
-- -- Roles via templates
-- SELECT r.*
-- FROM identity_role_template irt
--          JOIN role_template_roles rtr ON irt.role_template_id = rtr.role_template_id
--          JOIN roles r ON rtr.role_id = r.id
-- WHERE irt.identity_id = :identityId;