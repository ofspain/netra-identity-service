-- Add roles to template
CREATE OR REPLACE PROCEDURE add_roles_to_template(
    p_template_id BIGINT,
    p_role_ids BIGINT[]
)
LANGUAGE plpgsql
AS $$
BEGIN
INSERT INTO role_template_roles (role_template_id, role_id)
SELECT p_template_id, unnest(p_role_ids)
    ON CONFLICT DO NOTHING;
END;
$$;