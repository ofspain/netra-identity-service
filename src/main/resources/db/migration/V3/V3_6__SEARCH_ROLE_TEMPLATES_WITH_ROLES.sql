CREATE OR REPLACE FUNCTION get_role_template_with_roles(
    p_id BIGINT DEFAULT NULL,
    p_name VARCHAR DEFAULT NULL,
    p_limit INTEGER DEFAULT 10,
    p_offset INTEGER DEFAULT 0
)
RETURNS JSONB
LANGUAGE plpgsql
AS $$
DECLARE
result JSONB;
    total_count INTEGER;
BEGIN
    -- Get total count for pagination metadata
SELECT COUNT(*) INTO total_count
FROM role_templates rt
WHERE
    (p_id IS NULL OR rt.id = p_id)
  AND
    (p_name IS NULL OR rt.name = p_name);

-- Get paginated results with roles
SELECT jsonb_build_object(
               'data', COALESCE(
                (SELECT jsonb_agg(
                                jsonb_build_object(
                                        'template', to_jsonb(rt),
                                        'roles', COALESCE(
                                                (SELECT jsonb_agg(to_jsonb(r))
                                                 FROM roles r
                                                          JOIN role_template_roles rtr ON r.id = rtr.role_id
                                                 WHERE rtr.role_template_id = rt.id),
                                                '[]'::jsonb
                                                 )
                                )
                        )
                 FROM role_templates rt
                 WHERE
                     (p_id IS NULL OR rt.id = p_id)
                   AND
                     (p_name IS NULL OR rt.name = p_name)
                 ORDER BY rt.id
                LIMIT p_limit
                OFFSET p_offset),
            '[]'::jsonb
        ),
               'meta', jsonb_build_object(
                       'total', total_count,
                       'limit', p_limit,
                       'offset', p_offset
                       )
       ) INTO result;

RETURN result;
END;
$$;