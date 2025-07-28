CREATE OR REPLACE FUNCTION get_identities_with_roles_filtered(
    p_locked BOOLEAN DEFAULT NULL,
    p_disabled BOOLEAN DEFAULT NULL,
    p_domain_code VARCHAR DEFAULT NULL,
    p_created_start_date TIMESTAMP DEFAULT NULL,
    p_created_end_date TIMESTAMP DEFAULT NULL
)
RETURNS TABLE (
    id BIGINT,
    username VARCHAR,
    domain_code VARCHAR,
    disabled BOOLEAN,
    locked BOOLEAN,
    password_last_changed TIMESTAMP,
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
    WITH filtered_identities AS (
        SELECT
            d.id,
            d.username,
            d.domain_code,
            d.disabled,
            d.locked,
            d.password_last_changed,
            d.created_at,
            d.updated_at
        FROM identities d
        WHERE
            (p_created_start_date IS NULL OR d.created_at >= p_created_start_date)
            AND (p_created_end_date IS NULL OR d.created_at < p_created_end_date)
            AND (p_locked IS NULL OR d.locked = p_locked)
            AND (p_disabled IS NULL OR d.disabled = p_disabled)
            AND (p_domain_code IS NULL OR d.domain_code = p_domain_code)
    ),

    combined_roles AS (
        SELECT
            fi.id AS identity_id,
            r.id AS role_id,
            r.name AS role_name,
            r.description AS role_description,
            CASE WHEN ir.identity_id IS NOT NULL THEN TRUE ELSE FALSE END AS is_direct_role,
            CASE WHEN irt.identity_id IS NOT NULL THEN TRUE ELSE FALSE END AS is_template_role
        FROM filtered_identities fi

        -- Direct roles
        LEFT JOIN LATERAL (
            SELECT r.id, r.name, r.description, ir.identity_id
            FROM roles r
            JOIN identity_role ir ON ir.role_id = r.id
            WHERE ir.identity_id = fi.id
        ) ir ON TRUE

        -- Template roles
        LEFT JOIN LATERAL (
            SELECT r.id, r.name, r.description, irt.identity_id
            FROM roles r
            JOIN role_template_roles rtr ON rtr.role_id = r.id
            JOIN identity_role_template irt ON irt.role_template_id = rtr.role_template_id
            WHERE irt.identity_id = fi.id
        ) irt ON TRUE

        WHERE ir.identity_id IS NOT NULL OR irt.identity_id IS NOT NULL
    ),

    deduplicated_roles AS (
        SELECT
            identity_id,
            role_id,
            role_name,
            role_description,
            BOOL_OR(is_direct_role) AS is_direct_role,
            BOOL_OR(is_template_role) AS is_template_role
        FROM combined_roles
        GROUP BY identity_id, role_id, role_name, role_description
    )

SELECT
    fi.id,
    fi.username,
    fi.domain_code,
    fi.disabled,
    fi.locked,
    fi.password_last_changed,
    fi.created_at,
    fi.updated_at,
    dr.role_id,
    dr.role_name,
    dr.role_description,
    dr.is_direct_role,
    dr.is_template_role
FROM filtered_identities fi
         LEFT JOIN deduplicated_roles dr ON fi.id = dr.identity_id
ORDER BY fi.id, dr.role_name;
END;
$$;