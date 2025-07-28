CREATE OR REPLACE FUNCTION get_password_history(
    p_identity_id BIGINT,
    p_limit INTEGER DEFAULT 5
)
RETURNS TABLE (
    id BIGINT,
    hashed_password VARCHAR(255),
    created_at TIMESTAMP,
    days_since_change INTEGER
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Validate inputs
    IF p_identity_id IS NULL THEN
        RAISE EXCEPTION 'Identity ID cannot be null';
END IF;

    IF p_limit <= 0 OR p_limit > 20 THEN
        RAISE EXCEPTION 'Limit must be between 1 and 20';
END IF;

    -- Return password history with additional useful information
RETURN QUERY
SELECT
    ph.id,
    ph.hashed_password,
    ph.created_at,
    EXTRACT(DAY FROM (NOW() - ph.created_at))::INTEGER AS days_since_change
FROM
    password_history ph
WHERE
    ph.identity_id = p_identity_id
ORDER BY
    ph.created_at DESC
    LIMIT
        p_limit;

-- Optional: Add notice about password age
IF NOT FOUND THEN
        RAISE NOTICE 'No password history found for identity %', p_identity_id;
END IF;
END;
$$;