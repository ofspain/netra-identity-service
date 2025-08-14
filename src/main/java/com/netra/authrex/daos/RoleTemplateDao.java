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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class RoleTemplateDao {

    private final JdbcClient jdbcClient;
    private final ObjectMapper objectMapper;

    public RoleTemplate createRoleTemplate(RoleTemplate template) {
        Long id = jdbcClient.sql("SELECT upsert_role_template(?, NULL, ?, ?)")
                .param(1, template.getName())
                .param(2, template.getDescription())
                .param(3, template.getRoleIds().toArray(Long[]::new))
                .query(Long.class)
                .single();

        template.setId(id);
        return template;
    }

    public RoleTemplate updateRoleTemplate(RoleTemplate template) {
        Long id = jdbcClient.sql("SELECT upsert_role_template(?, ?, ?, ?)")
                .param(1, template.getName())
                .param(2, template.getId())
                .param(3, template.getDescription())
                .param(4, template.getRoleIds().toArray(Long[]::new))
                .query(Long.class)
                .single();

        if (!id.equals(template.getId())) {
            throw new IllegalStateException("Failed to update template with ID: " + template.getId());
        }
        return template;
    }

    public void addRolesToTemplate(Long templateId, List<Long> roleIds) {
        jdbcClient.sql("CALL add_roles_to_template(?, ?)")
                .param(1, templateId)
                .param(2, roleIds.toArray(Long[]::new))
                .update();
    }

    public void removeRolesFromTemplate(Long templateId, List<Long> roleIds) {
        jdbcClient.sql("CALL remove_roles_from_template(?, ?)")
                .param(1, templateId)
                .param(2, roleIds.toArray(Long[]::new))
                .update();
    }

    public Page<RoleTemplate> searchTemplates(RoleAndTemplateSearchParam searchParam) {
        Long id = searchParam.getId();
        String name = searchParam.getName();
        int pageNum = searchParam.getPageNum();
        int pageSize = searchParam.getPageSize();

        Pageable pageable = PageRequest.of(pageNum, pageSize);


        String json = jdbcClient.sql("SELECT get_role_template_with_roles(?, ?, ?, ?) as result")
                .param(1, id)
                .param(2, name)
                .param(3, pageSize)
                .param(4, pageNum)
                .query(String.class)
                .single();

        try {
            JsonNode root = objectMapper.readTree(json);
            List<RoleTemplate> content = objectMapper.convertValue(root.path("data"),
                    new TypeReference<List<RoleTemplate>>() {});

            JsonNode meta = root.path("meta");
            return new PageImpl<>(
                    content,
                    pageable,
                    meta.path("total").asLong()
            );
        } catch (JsonProcessingException e) {
            throw new AppDataAccessException("Error parsing role templates", e);
        }
    }

    public Optional<RoleTemplate> findRoleTemplate(Long id, String name){
        String json = jdbcClient.sql("CALL get_role_template_with_roles(?, ?)")
                .param(1, id)
                .param(2, name)
                .query(String.class)
                .single();

        Optional<RoleTemplate> result = Optional.empty();


        try {
            JsonNode root = objectMapper.readTree(json);
            RoleTemplate template = objectMapper.convertValue(root.path("template"),
                    new TypeReference<RoleTemplate>() {}
            );

            if(null != template){
                List<Role> roles =  objectMapper.convertValue(root.path("roles"),
                        new TypeReference<List<Role>>() {}
                );

                template.setRoles(roles);

                result = Optional.of(template);
            }

        } catch (JsonProcessingException e) {
            throw new AppDataAccessException("Error parsing role templates", e);
        }

        return result;
    }
}