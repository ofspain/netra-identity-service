package com.netra.authrex.controllers;

import com.netra.authrex.dtos.IdentityRoleDto;
import com.netra.authrex.dtos.IdentitySearchParam;
import com.netra.authrex.dtos.IdentityWithRolesDto;
import com.netra.authrex.services.IdentityService;
import com.netra.commons.models.Identity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;
import com.netra.authrex.dtos.ChangePasswordRequest;

@RestController
@RequestMapping("/api/identities")
@RequiredArgsConstructor
public class IdentityController {
    private final IdentityService identityService;

    @PostMapping
    public ResponseEntity<Identity> createIdentity(@Valid @RequestBody Identity identity) {
        Identity createdIdentity = identityService.createIdentity(identity);
        return ResponseEntity.ok(createdIdentity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Identity> updateIdentity(
            @PathVariable Long id,
            @Valid @RequestBody Identity identity) {
        identity.setId(id); // Ensure the ID from path is used
        Identity updatedIdentity = identityService.updateIdentity(identity);
        return ResponseEntity.ok(updatedIdentity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Identity> getIdentityById(@PathVariable Long id) {
        Optional<Identity> identity = identityService.findIdentityById(id);
        return identity.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Identity> getIdentityByUsername(@PathVariable String username) {
        Optional<Identity> identity = identityService.findIdentityByUsername(username);
        return identity.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<IdentityWithRolesDto>> searchIdentities(
            @ModelAttribute IdentitySearchParam searchParam) {
        // Set default values if not provided
        if (searchParam.getPageNum() == null) {
            searchParam.setPageNum(0);
        }
        if (searchParam.getPageSize() == null) {
            searchParam.setPageSize(20);
        }

        // Validate page size
        if (searchParam.getPageSize() > 100) {
            searchParam.setPageSize(100);
        }

        Page<IdentityWithRolesDto> result = identityService.findIdentities(searchParam);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{identityId}/roles")
    public ResponseEntity<Void> assignRoleToIdentity(
            @PathVariable Long identityId,
            @RequestBody IdentityRoleDto roleDto) {
        identityService.assignRoleToIdentity(identityId, roleDto.getRoleId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{identityId}/roles/{roleId}")
    public ResponseEntity<Void> removeRoleFromIdentity(
            @PathVariable Long identityId,
            @PathVariable Long roleId) {
        identityService.removeRoleFromIdentity(identityId, roleId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{identityId}/role-templates")
    public ResponseEntity<Void> assignRoleTemplateToIdentity(
            @PathVariable Long identityId,
            @RequestBody IdentityRoleDto roleDto) {
        identityService.assignRoleTemplateToIdentity(identityId, roleDto.getRoleId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{identityId}/role-templates/{templateId}")
    public ResponseEntity<Void> removeRoleTemplateFromIdentity(
            @PathVariable Long identityId,
            @PathVariable Long templateId) {
        identityService.removeRoleTemplateFromIdentity(identityId, templateId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        identityService.changePassword(id, request.getNewPassword());
        return ResponseEntity.ok().build();
    }


}