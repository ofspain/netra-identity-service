package com.netra.authrex.daos;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netra.authrex.dtos.RoleAndTemplateSearchParam;
import com.netra.authrex.exceptions.AppDataAccessException;
import com.netra.commons.database.EnhancedBeanPropertyRowMapper;
import com.netra.commons.models.Role;
import com.netra.commons.models.RoleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleDao {

    private final JdbcClient jdbcClient;
    private final RoleRowMapper roleRowMapper = new RoleRowMapper();

    private final ObjectMapper objectMapper;


    public Role createRole(Role role) {
        Long id = jdbcClient.sql("SELECT upsert_role(NULL, ?, ?)")
                .param(1, role.getName())
                .param(2, role.getDescription())
                .query(Long.class)
                .single();

        role.setId(id);
        return role;
    }

    public Role updateRole(Role role) {
        Long id = jdbcClient.sql("SELECT upsert_role(?, ?, ?)")
                .param(1, role.getId())
                .param(2, role.getName())
                .param(3, role.getDescription())
                .query(Long.class)
                .single();

        if (!id.equals(role.getId())) {
            throw new IllegalStateException("Failed to update role with ID: " + role.getId());
        }
        return role;
    }
    public Page<Role> searchRoles(RoleAndTemplateSearchParam searchParam) {
        Long id = searchParam.getId();
        String name = searchParam.getName();
        int pageNum = searchParam.getPageNum();
        int pageSize = searchParam.getPageSize();

        Pageable pageable = PageRequest.of(pageNum, pageSize);


        String json =  jdbcClient.sql("SELECT * FROM find_roles(?, ?,?,?)")
                .param(1, id)
                .param(2, name)
                .param(3, pageSize)
                .param(4, pageNum)
                .query(String.class)
                .single();

        try {
            JsonNode root = objectMapper.readTree(json);
            List<Role> content = objectMapper.convertValue(root.path("data"),
                    new TypeReference<List<Role>>() {});

            return new PageImpl<>(
                    content,
                    pageable,
                    root.path("total_count").asLong()
            );
        } catch (JsonProcessingException e) {
            throw new AppDataAccessException("Error parsing role templates", e);
        }
    }

    public Optional<Role> findRole(Long id, String name){
        return jdbcClient.sql("CALL get_role(?, ?)")
                .param(1, id)
                .param(2, name)
                .query(new EnhancedBeanPropertyRowMapper<Role>())
                .optional();
    }

    private static class RoleRowMapper implements RowMapper<Role> {
        @Override
        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
            Role role = new Role();
            role.setId(rs.getLong("id"));
            role.setName(rs.getString("name"));
            role.setDescription(rs.getString("description"));
            role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            role.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return role;
        }
    }
}