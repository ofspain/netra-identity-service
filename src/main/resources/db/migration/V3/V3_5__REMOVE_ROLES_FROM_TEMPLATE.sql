CREATE OR REPLACE PROCEDURE remove_roles_from_template(
    p_template_id BIGINT,
    p_role_ids BIGINT[]
)
LANGUAGE plpgsql
AS $$
BEGIN
DELETE FROM role_template_roles
WHERE role_template_id = p_template_id
  AND role_id = ANY(p_role_ids);
END;
$$;