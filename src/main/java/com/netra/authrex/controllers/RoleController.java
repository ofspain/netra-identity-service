package com.netra.authrex.controllers;

import com.netra.authrex.dtos.ApiResponse;
import com.netra.authrex.dtos.RoleAndTemplateSearchParam;
import com.netra.authrex.services.RoleService;
import com.netra.commons.models.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ApiResponse<Role> createRole(@RequestBody Role role, HttpServletRequest request) {
        Role created = roleService.createRole(role);
        return ApiResponse.success(
                created,
                "Role created successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @PutMapping("/{id}")
    public ApiResponse<Role> updateRole(@PathVariable Long id, @RequestBody Role role, HttpServletRequest request) {
        role.setId(id);
        Role updated = roleService.updateRole(role);
        return ApiResponse.success(
                updated,
                "Role updated successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @GetMapping
    public ApiResponse<Page<Role>> searchRoles(RoleAndTemplateSearchParam searchParam, HttpServletRequest request) {
        Page<Role> result = roleService.searchRoles(searchParam);
        return ApiResponse.success(
                result,
                "Roles fetched successfully",
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<Role> getRole(@PathVariable Long id,
                                     @RequestParam(required = false) String name,
                                     HttpServletRequest request) {
        Optional<Role> role = roleService.findRole(id, name);
        return role
                .map(r -> ApiResponse.success(r, "Role fetched successfully", request.getRequestURI(), UUID.randomUUID().toString()))
                .orElse(ApiResponse.error("404", "Role not found", null, request.getRequestURI(), UUID.randomUUID().toString()));
    }
}
