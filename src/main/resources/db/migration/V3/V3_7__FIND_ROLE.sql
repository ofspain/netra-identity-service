CREATE OR REPLACE FUNCTION get_role(
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR DEFAULT NULL
)
RETURNS TABLE (
    id BIGINT,
    name VARCHAR,
    description TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Raise an exception if both parameters are null
    IF p_id IS NULL AND p_name IS NULL THEN
        RAISE EXCEPTION 'At least one of p_id or p_name must be provided';
END IF;

RETURN QUERY
SELECT r.id, r.name, r.description, r.created_at, r.updated_at
FROM roles r
WHERE
    (p_id IS NULL OR r.id = p_id)
  AND
    (p_name IS NULL OR r.name = p_name);
END;
$$;
