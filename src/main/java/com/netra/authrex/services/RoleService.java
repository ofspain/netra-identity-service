package com.netra.authrex.services;

import com.netra.authrex.daos.RoleDao;
import com.netra.authrex.dtos.RoleAndTemplateSearchParam;
import com.netra.commons.models.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {

    private final RoleDao roleDao;

    @Transactional
    public Role createRole(Role role) {
        // Business logic (e.g., check duplicates, validations) can go here
        return roleDao.createRole(role);
    }

    @Transactional
    public Role updateRole(Role role) {
        // Business logic: maybe check existence first
        return roleDao.updateRole(role);
    }

    public Page<Role> searchRoles(RoleAndTemplateSearchParam searchParam) {
        return roleDao.searchRoles(searchParam);
    }

    public Optional<Role> findRole(Long id, String name) {
        return roleDao.findRole(id, name);
    }
}

