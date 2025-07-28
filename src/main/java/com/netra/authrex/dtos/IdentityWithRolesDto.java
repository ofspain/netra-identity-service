package com.netra.authrex.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityWithRolesDto {
    private Long id;
    private String username;
    private String domainCode;
    private boolean disabled;
    private boolean locked;
    private LocalDateTime passwordLastChanged;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    private List<IdentityRoleDto> roles = new ArrayList<>();

    // Helper method to add roles
    public void addRole(IdentityRoleDto role) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        this.roles.add(role);
    }
}