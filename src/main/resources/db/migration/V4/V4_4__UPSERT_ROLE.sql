CREATE OR REPLACE FUNCTION upsert_role(
    p_name VARCHAR,
    p_id BIGINT DEFAULT NULL,
    p_description TEXT DEFAULT NULL
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_id BIGINT;
    v_role_name VARCHAR := TRIM(p_name); -- Just trim, no uppercase enforcement
BEGIN
    -- Basic validation (optional)
    IF v_role_name IS NULL OR v_role_name = '' THEN
        RAISE EXCEPTION 'Role name cannot be empty';
END IF;

    -- Optional: Add length validation if needed
    IF LENGTH(v_role_name) > 50 THEN
        RAISE EXCEPTION 'Role name cannot exceed 50 characters';
END IF;

    IF p_id IS NULL THEN
        -- Check for existing role (case-insensitive if desired)
        PERFORM 1 FROM roles WHERE LOWER(name) = LOWER(v_role_name);
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