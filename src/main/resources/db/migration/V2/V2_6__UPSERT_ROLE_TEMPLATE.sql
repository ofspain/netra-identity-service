CREATE OR REPLACE FUNCTION upsert_role_template(
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR,
    p_description TEXT DEFAULT NULL,
    p_role_ids BIGINT[] DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_id BIGINT;
 v_role_id BIGINT;
BEGIN
    IF p_name IS NULL THEN
        RAISE EXCEPTION 'Template name is required';
END IF;

    IF p_id IS NULL THEN
        -- Check for existing template
        PERFORM 1 FROM role_templates WHERE name = p_name;
        IF FOUND THEN
            RAISE EXCEPTION 'Role template already exists';
END IF;

INSERT INTO role_templates (
    name, description, created_at, updated_at
) VALUES (
             p_name,
             p_description,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         )
    RETURNING id INTO v_id;
ELSE
UPDATE role_templates
SET
    name = p_name,
    description = COALESCE(p_description, description),
    updated_at = CURRENT_TIMESTAMP
WHERE id = p_id
    RETURNING id INTO v_id;

IF NOT FOUND THEN
            RAISE EXCEPTION 'Role template not found with ID %', p_id;
END IF;

        -- Clear existing role associations
DELETE FROM role_template_roles WHERE role_template_id = v_id;
END IF;

    -- Add new role associations if specified
    IF p_role_ids IS NOT NULL THEN
        FOREACH v_role_id IN ARRAY p_role_ids LOOP
            INSERT INTO role_template_roles (role_template_id, role_id)
            VALUES (v_id, v_role_id)
            ON CONFLICT DO NOTHING;
END LOOP;
END IF;

RETURN v_id;
END;
$$;