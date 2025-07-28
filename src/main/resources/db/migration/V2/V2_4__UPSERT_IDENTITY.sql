CREATE OR REPLACE FUNCTION upsert_identity(
    p_id BIGINT DEFAULT NULL,
    p_username VARCHAR,
    p_password VARCHAR,
    p_disabled BOOLEAN DEFAULT false,
    p_locked BOOLEAN DEFAULT false,
    p_domain_code VARCHAR,
    p_password_last_changed TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
v_id BIGINT;
    v_username_lower VARCHAR := LOWER(TRIM(p_username));
BEGIN
    -- Validate inputs
    IF p_username IS NULL OR p_domain_code IS NULL THEN
        RAISE EXCEPTION 'Username and domain code are required';
    END IF;
    
    IF p_id IS NULL THEN
        -- Check for existing username
        PERFORM 1 FROM identities WHERE username = v_username_lower;
        IF FOUND THEN
            RAISE EXCEPTION 'Username already exists';
        END IF;

        INSERT INTO identities (
            username, password, disabled, locked, domain_code,
            password_last_changed, created_at, updated_at
        ) VALUES (
             v_username_lower,
             p_password,
             COALESCE(p_disabled, false),
             COALESCE(p_locked, false),
             p_domain_code,
             COALESCE(p_password_last_changed, CURRENT_TIMESTAMP),
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         )
        RETURNING id INTO v_id;
    ELSE
        UPDATE identities
        SET
            username = v_username_lower,
            password = COALESCE(p_password, password),
            disabled = COALESCE(p_disabled, disabled),
            locked = COALESCE(p_locked, locked),
            domain_code = p_domain_code,
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