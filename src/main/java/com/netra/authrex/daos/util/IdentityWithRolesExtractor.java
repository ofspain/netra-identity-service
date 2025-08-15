package com.netra.authrex.daos.util;

import com.netra.authrex.dtos.IdentityRoleDto;
import com.netra.authrex.dtos.IdentityWithRolesDto;
import com.netra.commons.exceptions.AppDataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ResultSetExtractor for converting flat identity-role rows into nested DTOs.
 *
 * Handles the complexity of grouping multiple rows per identity into a single
 * IdentityWithRolesDto object containing a list of roles.
 *
 * Expected ResultSet columns:
 * - id, username, domain_code, disabled, locked
 * - password_last_changed, created_at, updated_at
 * - role_id, role_name, role_description, has_direct_role, has_template_role
 */
public class IdentityWithRolesExtractor implements ResultSetExtractor<List<IdentityWithRolesDto>> {

    @Override
    public List<IdentityWithRolesDto> extractData(ResultSet rs) throws SQLException {
        Map<Long, IdentityWithRolesDto> identityMap = new LinkedHashMap<>();

        while (rs.next()) {
            Long identityId = rs.getLong("id");

            // Get or create identity
            IdentityWithRolesDto identity = identityMap.computeIfAbsent(
                    identityId,
                    id -> createIdentityFromResultSet(rs, identityId)
            );

            // Add role if present
            addRoleIfPresent(rs, identity);
        }

        return new ArrayList<>(identityMap.values());
    }

    private IdentityWithRolesDto createIdentityFromResultSet(ResultSet rs, Long identityId) {
        try {
            return IdentityWithRolesDto.builder()
                    .id(identityId)
                    .username(rs.getString("username"))
                    .domainCode(rs.getString("domain_code"))
                    .disabled(rs.getBoolean("disabled"))
                    .locked(rs.getBoolean("locked"))
                    .passwordLastChanged(toLocalDateTime(rs.getTimestamp("password_last_changed")))
                    .createdAt(toLocalDateTime(rs.getTimestamp("created_at")))
                    .updatedAt(toLocalDateTime(rs.getTimestamp("updated_at")))
                    .roles(new ArrayList<>())
                    .build();
        } catch (SQLException e) {
            throw new AppDataAccessException("Failed to create identity from ResultSet", e);
        }
    }

    private void addRoleIfPresent(ResultSet rs, IdentityWithRolesDto identity) throws SQLException {
        Long roleId = rs.getLong("role_id");

        if (!rs.wasNull() && roleId != null) {
            IdentityRoleDto roleDto = IdentityRoleDto.builder()
                    .roleId(roleId)
                    .roleName(rs.getString("role_name"))
                    .roleDescription(rs.getString("role_description"))
                    .directRole(rs.getBoolean("has_direct_role"))
                    .templateRole(rs.getBoolean("has_template_role"))
                    .build();

            identity.getRoles().add(roleDto);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }
}
