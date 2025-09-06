CREATE OR REPLACE FUNCTION find_roles(
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR DEFAULT NULL,
    p_limit INTEGER DEFAULT 50,
    p_offset INTEGER DEFAULT 0
)
RETURNS JSON AS
$$
DECLARE
total_count BIGINT;
    result JSON;
BEGIN
    -- Get total count
SELECT COUNT(*) INTO total_count
FROM roles r
WHERE
    (p_id IS NULL OR r.id = p_id)
  AND
    (p_name IS NULL OR r.name = p_name);

-- Build result as JSON
SELECT jsonb_build_object(
               'total_count', total_count,
               'data', COALESCE(jsonb_agg(to_jsonb(role_row)), '[]'::jsonb)
       )
INTO result
FROM (
         SELECT r.id, r.name, r.description, r.created_at, r.updated_at
         FROM roles r
         WHERE
             (p_id IS NULL OR r.id = p_id)
           AND
             (p_name IS NULL OR r.name = p_name)
         ORDER BY r.id
             LIMIT p_limit
         OFFSET p_offset
     ) AS role_row;

RETURN result;
END;
$$ LANGUAGE plpgsql;
