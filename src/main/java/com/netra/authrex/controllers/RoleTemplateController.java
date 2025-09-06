package com.netra.authrex.controllers;

import com.netra.authrex.dtos.ApiResponse;
import com.netra.authrex.dtos.RoleAndTemplateSearchParam;
import com.netra.authrex.services.RoleTemplateService;
import com.netra.commons.models.RoleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/role-templates")
@RequiredArgsConstructor
public class RoleTemplateController {

    private final RoleTemplateService roleTemplateService;

    @PostMapping
    public ApiResponse<RoleTemplate> createTemplate(@RequestBody RoleTemplate template,
                                                    HttpServletRequest request) {
        RoleTemplate created = roleTemplateService.createRoleTemplate(template);
        return ApiResponse.success(
                created,
                "Role template created successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleTemplate> updateTemplate(@PathVariable Long id,
                                                    @RequestBody RoleTemplate template,
                                                    HttpServletRequest request) {
        template.setId(id);
        RoleTemplate updated = roleTemplateService.updateRoleTemplate(template);
        return ApiResponse.success(
                updated,
                "Role template updated successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @PostMapping("/{id}/roles")
    public ApiResponse<Void> addRolesToTemplate(@PathVariable Long id,
                                                @RequestBody List<Long> roleIds,
                                                HttpServletRequest request) {
        roleTemplateService.addRolesToTemplate(id, roleIds);
        return ApiResponse.success(
                null,
                "Roles added to template successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @DeleteMapping("/{id}/roles")
    public ApiResponse<Void> removeRolesFromTemplate(@PathVariable Long id,
                                                     @RequestBody List<Long> roleIds,
                                                     HttpServletRequest request) {
        roleTemplateService.removeRolesFromTemplate(id, roleIds);
        return ApiResponse.success(
                null,
                "Roles removed from template successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @GetMapping
    public ApiResponse<Page<RoleTemplate>> searchTemplates(RoleAndTemplateSearchParam searchParam,
                                                           HttpServletRequest request) {
        Page<RoleTemplate> result = roleTemplateService.searchTemplates(searchParam);
        return ApiResponse.success(
                result,
                "Role templates fetched successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleTemplate> getTemplate(@PathVariable Long id,
                                                 @RequestParam(required = false) String name,
                                                 HttpServletRequest request) {
        Optional<RoleTemplate> template = roleTemplateService.findRoleTemplate(id, name);
        return template
                .map(t -> ApiResponse.success(t,
                        "Role template fetched successfully",
                        request.getRequestURI(),
                        UUID.randomUUID().toString()))
                .orElse(ApiResponse.error(
                        "404",
                        "Role template not found",
                        null,
                        request.getRequestURI(),
                        UUID.randomUUID().toString()
                ));
    }
}
