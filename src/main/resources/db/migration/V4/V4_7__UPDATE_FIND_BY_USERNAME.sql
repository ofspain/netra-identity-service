DROP FUNCTION IF EXISTS get_identity_with_roles(VARCHAR);

CREATE OR REPLACE FUNCTION get_identity_with_roles(p_username VARCHAR)

RETURNS TABLE (
    id BIGINT,
    username VARCHAR,
    password VARCHAR,               -- needed for Spring Security
    identity_uuid VARCHAR,             -- expose to hydrate Identity
    domain_code VARCHAR,
    domain_type VARCHAR,            -- for enum mapping
    disabled BOOLEAN,
    locked BOOLEAN,
    password_last_changed TIMESTAMP,
    last_login TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
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
    WITH identity_data AS (
            SELECT
                i.id,
                i.username,
                i.password,         -- added
                i.identity_uuid,    -- added
                i.domain_code,
                i.domain_type,      -- added
                i.disabled,
                i.locked,
                i.password_last_changed,
                i.last_login,
                i.created_at,
                i.updated_at
            FROM identities i
            WHERE i.username = p_username
            LIMIT 1
        ),
        role_assignments AS (
            -- Direct roles
            SELECT
                r.id,
                r.name,
                r.description,
                TRUE AS is_direct,
                FALSE AS is_template
            FROM identity_data i
            JOIN identity_role ir ON i.id = ir.identity_id
            JOIN roles r ON ir.role_id = r.id

            UNION ALL

            -- Template roles
            SELECT
                r.id,
                r.name,
                r.description,
                FALSE AS is_direct,
                TRUE AS is_template
            FROM identity_data i
            JOIN identity_role_template irt ON i.id = irt.identity_id
            JOIN role_template_roles rtr ON irt.role_template_id = rtr.role_template_id
            JOIN roles r ON rtr.role_id = r.id
        ),
        deduplicated_roles AS (
            SELECT
                ra.id,
                ra.name,
                ra.description,
                BOOL_OR(ra.is_direct)   AS is_direct_role,
                BOOL_OR(ra.is_template) AS is_template_role
            FROM role_assignments ra
            GROUP BY ra.id, ra.name, ra.description
        )
SELECT
    i.id,
    i.username,
    i.password,          -- added
    i.identity_uuid,     -- added
    i.domain_code,
    i.domain_type,       -- added
    i.disabled,
    i.locked,
    i.password_last_changed,
    i.last_login,
    i.created_at,
    i.updated_at,
    dr.id AS role_id,
    dr.name AS role_name,
    dr.description AS role_description,
    dr.is_direct_role,
    dr.is_template_role
FROM identity_data i
         LEFT JOIN deduplicated_roles dr ON TRUE
ORDER BY dr.name;
END;
$$;
