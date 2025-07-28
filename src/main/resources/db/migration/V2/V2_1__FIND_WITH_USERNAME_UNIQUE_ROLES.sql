CREATE OR REPLACE FUNCTION get_identity_with_roles(p_username VARCHAR)
RETURNS TABLE (
    -- Identity fields
    id BIGINT,
    username VARCHAR,
    domain_code VARCHAR,
    disabled BOOLEAN,
    locked BOOLEAN,
    password_last_changed TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,

    -- Role fields (deduplicated)
    role_id BIGINT,
    role_name VARCHAR,
    role_description TEXT,
    is_direct_role BOOLEAN,
    is_template_role BOOLEAN
)
LANGUAGE plpgsql
AS $$
BEGIN
RETURN QUERY
    -- First get the identity record
    WITH identity_data AS (
        SELECT
            id,
            username,
            domain_code,
            disabled,
            locked,
            password_last_changed,
            created_at,
            updated_at
        FROM identities
        WHERE username = p_username
        LIMIT 1
    ),

    -- Then collect all distinct roles with their sources
    role_assignments AS (
        -- Direct role assignments
        SELECT
            r.id,
            r.name,
            r.description,
            TRUE AS is_direct,
            FALSE AS is_template
        FROM identity_data id
        JOIN identity_role ir ON id.id = ir.identity_id
        JOIN roles r ON ir.role_id = r.id

        UNION ALL

        -- Template-based role assignments
        SELECT
            r.id,
            r.name,
            r.description,
            FALSE AS is_direct,
            TRUE AS is_template
        FROM identity_data id
        JOIN identity_role_template irt ON id.id = irt.identity_id
        JOIN role_template_roles rtr ON irt.role_template_id = rtr.role_template_id
        JOIN roles r ON rtr.role_id = r.id
    ),

    -- Deduplicate roles while preserving source info
    deduplicated_roles AS (
        SELECT
            id,
            name,
            description,
            BOOL_OR(is_direct) AS is_direct_role,
            BOOL_OR(is_template) AS is_template_role
        FROM role_assignments
        GROUP BY id, name, description
    )

-- Combine identity with deduplicated roles
SELECT
    id.id,
    id.username,
    id.domain_code,
    id.disabled,
    id.locked,
    id.password_last_changed,
    id.created_at,
    id.updated_at,
    dr.id AS role_id,
    dr.name AS role_name,
    dr.description AS role_description,
    dr.is_direct_role,
    dr.is_template_role
FROM identity_data id
         LEFT JOIN deduplicated_roles dr ON TRUE
ORDER BY dr.name;
END;
$$;