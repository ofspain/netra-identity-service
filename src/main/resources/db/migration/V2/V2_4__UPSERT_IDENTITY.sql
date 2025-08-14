CREATE OR REPLACE FUNCTION upsert_identity(
    p_domain_code VARCHAR,
    p_username VARCHAR,
    p_password VARCHAR,
    p_domain_type VARCHAR,
    p_identity_uuid VARCHAR,
    p_id BIGINT DEFAULT NULL,
    p_disabled BOOLEAN DEFAULT false,
    p_locked BOOLEAN DEFAULT false,
    p_password_last_changed TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_id BIGINT;
    v_username_lower VARCHAR := LOWER(TRIM(p_username));
BEGIN
    -- Validate required input
    IF p_username IS NULL THEN
        RAISE EXCEPTION 'Username is required';
END IF;

    IF p_id IS NULL THEN
        -- Inserting: validate required fields
        IF p_domain_code IS NULL OR p_domain_type IS NULL OR p_identity_uuid IS NULL THEN
            RAISE EXCEPTION 'Domain code, domain type, and identity UUID are required for insert';
END IF;

        -- Check for existing username
        PERFORM 1 FROM identities WHERE username = v_username_lower;
        IF FOUND THEN
            RAISE EXCEPTION 'Username already exists';
END IF;

INSERT INTO identities (
    username, password, disabled, locked, domain_code, domain_type, identity_uuid,
    password_last_changed, created_at, updated_at
) VALUES (
             v_username_lower,
             p_password,
             COALESCE(p_disabled, false),
             COALESCE(p_locked, false),
             p_domain_code,
             p_domain_type,
             p_identity_uuid,
             COALESCE(p_password_last_changed, CURRENT_TIMESTAMP),
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         )
    RETURNING id INTO v_id;

ELSE
        -- Updating: do not touch immutable fields
UPDATE identities
SET
    username = v_username_lower,
    password = COALESCE(p_password, password),
    disabled = COALESCE(p_disabled, disabled),
    locked = COALESCE(p_locked, locked),
    password_last_changed = CASE
                                WHEN p_password IS NOT NULL THEN CURRENT_TIMESTAMP
                                ELSE password_last_changed
        END,
    updated_at = CURRENT_TIMESTAMP
WHERE id = p_id
    RETURNING id INTO v_id;

IF NOT FOUND THEN
            RAISE EXCEPTION 'Identity not found with ID %', p_id;
END IF;
END IF;

RETURN v_id;
END;
$$;
