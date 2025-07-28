CREATE TABLE role_template_roles (
    role_template_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (role_template_id, role_id),
    FOREIGN KEY (role_template_id) REFERENCES role_templates(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);