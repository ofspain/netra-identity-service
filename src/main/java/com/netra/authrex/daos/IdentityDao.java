package com.netra.authrex.daos;

import com.netra.authrex.daos.util.IdentityWithRolesExtractor;
import com.netra.authrex.dtos.IdentitySearchParam;
import com.netra.authrex.dtos.IdentityWithRolesDto;
import com.netra.commons.database.EnhancedBeanPropertyRowMapper;
import com.netra.commons.models.Identity;
import com.netra.commons.util.TraceableUuidGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdentityDao {

    private final JdbcClient jdbcClient;
    private final IdentityWithRolesExtractor identityExtractor = new IdentityWithRolesExtractor();

    private final PasswordEncoder passwordEncoder;

    public Optional<Identity> loadIdentityByUsername(String username){

        return jdbcClient.sql("SELECT * FROM get_identity_with_roles(?)")
                .param(1, username)
                .query(new EnhancedBeanPropertyRowMapper<Identity>())
                .optional();
    }


    public Page<IdentityWithRolesDto> findIdentities(IdentitySearchParam searchParam) {
        int pageNumber = searchParam.getPageNum();
        int pageSize = searchParam.getPageSize();
        int offset = pageNumber * pageSize;

        // Get total count
        Long totalCount = getTotalIdentityCount(searchParam);
        if (totalCount == 0) {
            return createEmptyPage(pageNumber, pageSize);
        }

        // Fetch paginated data
        String sql = """
            SELECT * FROM (
                SELECT * FROM get_identities_with_roles_filtered(?, ?, ?, ?, ?)
            ) AS result
            ORDER BY id, role_name
            OFFSET ? LIMIT ?
            """;

        List<IdentityWithRolesDto> content = jdbcClient.sql(sql)
                .param(1, searchParam.getLocked())
                .param(2, searchParam.getDisabled())
                .param(3, searchParam.getDomainCode())
                .param(4, searchParam.getStartDate())
                .param(5, searchParam.getEndDate())
                .param(6, offset)
                .param(7, pageSize)
                .query(identityExtractor);

        return new PageImpl<>(content, PageRequest.of(pageNumber, pageSize), totalCount);
    }

    /**
     * Get total count of unique identities (not rows)
     */
    private Long getTotalIdentityCount(IdentitySearchParam searchParam) {
        String countSql = """
            SELECT COUNT(DISTINCT id) 
            FROM get_identities_with_roles_filtered(?, ?, ?, ?, ?)
            """;

        return jdbcClient.sql(countSql)
                .param(1, searchParam.getLocked())
                .param(2, searchParam.getDisabled())
                .param(3, searchParam.getDomainCode())
                .param(4, searchParam.getStartDate())
                .param(5, searchParam.getEndDate())
                .query(Long.class)
                .single();
    }

    /**
     * Create empty page for zero results
     */
    private Page<IdentityWithRolesDto> createEmptyPage(int pageNumber, int pageSize) {
        return new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(pageNumber, pageSize),
                0
        );
    }

    public Identity saveIdentity(Identity identity) {
        Long id = jdbcClient.sql("""
            SELECT upsert_identity(
                ?, ?, ?, ?, ?, NULL, ?, ?, ?
            )
        """)
                .param(1, identity.getDomainCode())
                .param(2, identity.getUsername())
                .param(3, passwordEncoder.encode(identity.getPassword()))
                .param(4, identity.getDomainType())
                .param(5, TraceableUuidGenerator.generateTraceableUuid(identity.getDomainCode()))
                .param(6, identity.getDisabled())
                .param(7, identity.getLocked())
                .param(8, identity.getPasswordLastChanged())
                .query(Long.class)
                .single();

        identity.setId(id);
        return identity;
    }



    //todo: please reanalysze the updating the password with identity itself
    //todo: please reanalysze the updating the password with identity itself
    //todo: please reanalysze the updating the password with identity itself
    //todo: please reanalysze the updating the password with identity itself
    //todo: please reanalysze the updating the password with identity itself
    //todo: please reanalysze the updating the password with identity itself
    public Identity updateIdentity(Identity identity, Boolean updatePassword) {
       // boolean updatePassword = identity.getPassword() != null && !identity.getPassword().isBlank();

        Long id = jdbcClient.sql("""
            SELECT upsert_identity(
                ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
        """)
            .param(1, identity.getDomainCode())     // required even in update due to function signature
            .param(2, identity.getUsername())       // p_username
            .param(3, updatePassword ? passwordEncoder.encode(identity.getPassword()) : null)  // p_password
            .param(4, identity.getDomainType())     // p_domain_type
            .param(5, identity.getIdentityUuid())   // make sure you pass this!
            .param(6, identity.getId())             // p_id
            .param(7, identity.getDisabled())       // p_disabled
            .param(8, identity.getLocked())         // p_locked
            .param(9, identity.getPasswordLastChanged()) // optional, but useful
            .query(Long.class)
            .single();


        if (!id.equals(identity.getId())) {
            throw new IllegalStateException("Failed to update identity with ID: " + identity.getId());
        }

        return identity;
    }


    public void assignRoleToIdentity(Long identityId, Long roleId) {
        jdbcClient.sql("""
                INSERT INTO identity_role (identity_id, role_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """)
                .param(1, identityId)
                .param(2, roleId)
                .update();
    }

    public void removeRoleFromIdentity(Long identityId, Long roleId) {
        jdbcClient.sql("""
                DELETE FROM identity_role
                WHERE identity_id = ? AND role_id = ?
                """)
                .param(1, identityId)
                .param(2, roleId)
                .update();
    }

    public void assignRoleTemplateToIdentity(Long identityId, Long templateId) {
        jdbcClient.sql("""
                INSERT INTO identity_role_template (identity_id, role_template_id)
                VALUES (?, ?)
                ON CONFLICT DO NOTHING
                """)
                .param(1, identityId)
                .param(2, templateId)
                .update();
    }

    public void removeRoleTemplateFromIdentity(Long identityId, Long templateId) {
        jdbcClient.sql("""
                DELETE FROM identity_role_template
                WHERE identity_id = ? AND role_template_id = ?
                """)
                .param(1, identityId)
                .param(2, templateId)
                .update();
    }

    public Optional<Identity> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM identities WHERE id = ?")
                .param(1, id)
                .query(new EnhancedBeanPropertyRowMapper<Identity>())
                .optional();
    }

    public Optional<Identity> findByUsername(String username) {
        return jdbcClient.sql("""
                SELECT i.* FROM identities i 
                WHERE i.username = ? AND i.locked = false
                """)
                .param(1, username)
                .query(new EnhancedBeanPropertyRowMapper<Identity>())
                .optional();
    }

    public Optional<Identity> findByUsernameAndDomain(String username, String domainCode) {
        return jdbcClient.sql("""
                SELECT i.* FROM identities i 
                WHERE i.username = ? AND i.domain_code = ? AND i.locked = false
                """)
                .param(1, username)
                .param(2, domainCode)
                .query(new EnhancedBeanPropertyRowMapper<Identity>())
                .optional();
    }

    public void updateLastLogin(Long identityId) {
        jdbcClient.sql("UPDATE identities SET last_login = NOW() WHERE id = ?")
                .param(1, identityId)
                .update();
    }
}
