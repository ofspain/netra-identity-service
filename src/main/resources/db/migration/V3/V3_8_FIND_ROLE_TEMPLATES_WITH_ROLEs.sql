CREATE OR REPLACE FUNCTION get_role_template_with_roles(
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
result JSONB;
BEGIN
    -- Validate input
    IF p_id IS NULL AND p_name IS NULL THEN
        RAISE EXCEPTION 'At least one of p_id or p_name must be provided';
END IF;

    -- Fetch template and roles
SELECT jsonb_build_object(
               'template', to_jsonb(rt),
               'roles', COALESCE(
                       (
                           SELECT jsonb_agg(to_jsonb(r))
                              FROM roles r
                              JOIN role_template_roles rtr ON r.id = rtr.role_id
                              WHERE rtr.role_template_id = rt.id
                       ),
                       '[]'::jsonb
                        )
       )
INTO result
FROM role_templates rt
WHERE
    (p_id IS NULL OR rt.id = p_id)
  AND
    (p_name IS NULL OR rt.name = p_name)
    LIMIT 1;

-- If no result is found
IF result IS NULL THEN
        RETURN jsonb_build_object(
            'template', NULL,
            'roles', '[]'::jsonb
        );
END IF;

RETURN result;
END;
$$;
