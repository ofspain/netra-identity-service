package com.netra.authrex.controllers;

import com.netra.authrex.dtos.*;
import com.netra.authrex.services.IdentityService;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.Identity;
import com.netra.commons.requests.CreateIdentityRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/identities")
@RequiredArgsConstructor
public class IdentityController {
    private final IdentityService identityService;

    @PostMapping("/registration")
    public ApiResponse<Identity> registerIdentity(@RequestBody CreateIdentityRequest identityRequest) {
        Identity identity = identityRequest.toIdentity();
        identity.setDomainCode(Identity.CUSTOMERUSER_DOMAINCODE);
        identity.setDomainType(DomainType.CUSTOMER);
        System.out.println("PASSWORD "+identity.getPassword());
        Identity createdIdentity = identityService.createIdentity(identity);

        System.out.println("Created User "+identity);
        return ApiResponse.success(createdIdentity, "User registered successfully",
                "/api/identities/registration", "trace-id-placeholder");
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_DOMAIN')")
    public ApiResponse<Identity> createIdentity(@RequestBody CreateIdentityRequest identityRequest) {
        Identity identity = identityRequest.toIdentity();
        Identity createdIdentity = identityService.createIdentity(identity);
        return ApiResponse.success(createdIdentity, "User created successfully",
                "/api/identities/create", "trace-id-placeholder");
    }

    @PutMapping("/{id}")
    public ApiResponse<Identity> updateIdentity(
            @PathVariable Long id,
            @Valid @RequestBody Identity identity) {
        identity.setId(id);
        Identity updatedIdentity = identityService.updateIdentity(identity);
        return ApiResponse.success(updatedIdentity, "User updated successfully",
                "/api/identities/" + id, "trace-id-placeholder");
    }

    @GetMapping("/{id}")
    public ApiResponse<Identity> getIdentityById(@PathVariable Long id) {
        Optional<Identity> identity = identityService.findIdentityById(id);
        return identity
                .map(i -> ApiResponse.success(i, "User found", "/api/identities/" + id, "trace-id-placeholder"))
                .orElseGet(() -> ApiResponse.error("404", "User not found", null, "/api/identities/" + id, "trace-id-placeholder"));
    }

    @GetMapping("/username/{username}")
    public ApiResponse<Identity> getIdentityByUsername(@PathVariable String username) {
        Optional<Identity> identity = identityService.findIdentityByUsername(username);
        return identity
                .map(i -> ApiResponse.success(i, "User found", "/api/identities/username/" + username, "trace-id-placeholder"))
                .orElseGet(() -> ApiResponse.error("404", "User not found", null, "/api/identities/username/" + username, "trace-id-placeholder"));
    }

    @GetMapping("/search")
    public ApiResponse<Page<IdentityWithRolesDto>> searchIdentities(
            @ModelAttribute IdentitySearchParam searchParam) {

        // No manual pageNum/pageSize checks needed
        Page<IdentityWithRolesDto> result = identityService.findIdentities(searchParam);

        return ApiResponse.success(
                result,
                "Search completed",
                "/api/identities/search",
                "trace-id-placeholder"
        );
    }

    @PostMapping("/{identityId}/roles")
    public ApiResponse<Void> assignRoleToIdentity(
            @PathVariable Long identityId,
            @RequestBody IdentityRoleDto roleDto) {
        identityService.assignRoleToIdentity(identityId, roleDto.getRoleId());
        return ApiResponse.success(null, "Role assigned successfully",
                "/api/identities/" + identityId + "/roles", "trace-id-placeholder");
    }

    @DeleteMapping("/{identityId}/roles/{roleId}")
    public ApiResponse<Void> removeRoleFromIdentity(
            @PathVariable Long identityId,
            @PathVariable Long roleId) {
        identityService.removeRoleFromIdentity(identityId, roleId);
        return ApiResponse.success(null, "Role removed successfully",
                "/api/identities/" + identityId + "/roles/" + roleId, "trace-id-placeholder");
    }

    @PostMapping("/{identityId}/role-templates")
    public ApiResponse<Void> assignRoleTemplateToIdentity(
            @PathVariable Long identityId,
            @RequestBody IdentityRoleDto roleDto) {
        identityService.assignRoleTemplateToIdentity(identityId, roleDto.getRoleId());
        return ApiResponse.success(null, "Role template assigned successfully",
                "/api/identities/" + identityId + "/role-templates", "trace-id-placeholder");
    }

    @DeleteMapping("/{identityId}/role-templates/{templateId}")
    public ApiResponse<Void> removeRoleTemplateFromIdentity(
            @PathVariable Long identityId,
            @PathVariable Long templateId) {
        identityService.removeRoleTemplateFromIdentity(identityId, templateId);
        return ApiResponse.success(null, "Role template removed successfully",
                "/api/identities/" + identityId + "/role-templates/" + templateId, "trace-id-placeholder");
    }

    @PostMapping("/{id}/change-password")
    public ApiResponse<Void> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        identityService.changePassword(id, request.getNewPassword());
        return ApiResponse.success(null, "Password changed successfully",
                "/api/identities/" + id + "/change-password", "trace-id-placeholder");
    }
}
