package com.netra.authrex.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityRoleDto {
    private Long roleId;
    private String roleName;
    private String roleDescription;
    private boolean directRole;
    private boolean templateRole;
}
