package com.netra.authrex.services;

import com.netra.authrex.daos.RoleTemplateDao;
import com.netra.authrex.dtos.RoleAndTemplateSearchParam;
import com.netra.commons.models.RoleTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleTemplateService {

    private final RoleTemplateDao roleTemplateDao;

    @Transactional
    public RoleTemplate createRoleTemplate(RoleTemplate template) {
        // business logic (e.g., validate name uniqueness) can go here
        return roleTemplateDao.createRoleTemplate(template);
    }

    @Transactional
    public RoleTemplate updateRoleTemplate(RoleTemplate template) {
        return roleTemplateDao.updateRoleTemplate(template);
    }

    @Transactional
    public void addRolesToTemplate(Long templateId, List<Long> roleIds) {
        roleTemplateDao.addRolesToTemplate(templateId, roleIds);
    }

    @Transactional
    public void removeRolesFromTemplate(Long templateId, List<Long> roleIds) {
        roleTemplateDao.removeRolesFromTemplate(templateId, roleIds);
    }

    public Page<RoleTemplate> searchTemplates(RoleAndTemplateSearchParam searchParam) {
        return roleTemplateDao.searchTemplates(searchParam);
    }

    public Optional<RoleTemplate> findRoleTemplate(Long id, String name) {
        return roleTemplateDao.findRoleTemplate(id, name);
    }
}
