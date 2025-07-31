-- Create or update role
CREATE OR REPLACE FUNCTION upsert_role(
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR,
    p_description TEXT DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_id BIGINT;
    v_role_name VARCHAR := UPPER(TRIM(p_name));
BEGIN
    -- Validate role name format
    IF v_role_name !~ '^ROLE_[A-Z0-9_]+$' THEN
        RAISE EXCEPTION 'Role name must start with ROLE_ and contain only uppercase letters, numbers and underscores';
END IF;

    IF p_id IS NULL THEN
        -- Check for existing role
        PERFORM 1 FROM roles WHERE name = v_role_name;
        IF FOUND THEN
            RAISE EXCEPTION 'Role already exists';
END IF;

INSERT INTO roles (name, description, created_at, updated_at)
VALUES (v_role_name, p_description, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    RETURNING id INTO v_id;
ELSE
UPDATE roles
SET
    name = v_role_name,
    description = COALESCE(p_description, description),
    updated_at = CURRENT_TIMESTAMP
WHERE id = p_id
    RETURNING id INTO v_id;

IF NOT FOUND THEN
            RAISE EXCEPTION 'Role not found with ID %', p_id;
END IF;
END IF;

RETURN v_id;
END;
$$;