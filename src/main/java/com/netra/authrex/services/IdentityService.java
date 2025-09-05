package com.netra.authrex.services;

import com.netra.authrex.daos.IdentityDao;
import com.netra.authrex.dtos.AuthUser;
import com.netra.authrex.dtos.IdentityRoleDto;
import com.netra.authrex.dtos.IdentitySearchParam;
import com.netra.authrex.dtos.IdentityWithRolesDto;
import com.netra.commons.models.Identity;
import com.netra.commons.models.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityService implements UserDetailsService {

    private final IdentityDao identityDao;

    @Value("${max.days.allowed.password.lifetime}")
    private String passwordExpiryDays;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Identity user = identityDao.loadIdentityByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (isPasswordExpired(user)) {
            throw new CredentialsExpiredException("Password has expired");
        }

        return new AuthUser(user);
    }


    private Collection<? extends GrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    public boolean isPasswordExpired(Identity user) {
        return user.getPasswordLastChanged()
                .isBefore(LocalDateTime.now().minusDays(Long.getLong(passwordExpiryDays, 90)));
    }

    public void checkDomainCompatibility(Identity user, String requestedDomain) {
        if (!user.getDomainCode().equals(requestedDomain)) {
            throw new UsernameNotFoundException("User not found in specified domain "+requestedDomain);
        }
    }

    public Page<IdentityWithRolesDto> findIdentities(IdentitySearchParam searchParam) {
        return identityDao.findIdentities(searchParam);
    }

    @Transactional
    public Identity createIdentity(Identity identity) {
        return identityDao.saveIdentity(identity);
    }

    @Transactional
    public Identity updateIdentity(Identity identity) {
        return identityDao.updateIdentity(identity, false);
    }

    @Transactional
    public void assignRoleToIdentity(Long identityId,  Long roleId) {
        identityDao.assignRoleToIdentity(identityId, roleId);
    }

    @Transactional
    public void removeRoleFromIdentity(Long identityId,  Long roleId) {
        identityDao.removeRoleFromIdentity(identityId, roleId);
    }

    @Transactional
    public void assignRoleTemplateToIdentity(Long identityId,  Long roleId) {
        identityDao.assignRoleTemplateToIdentity(identityId, roleId);
    }

    @Transactional
    public void removeRoleTemplateFromIdentity(Long identityId,  Long roleId) {
        identityDao.removeRoleTemplateFromIdentity(identityId, roleId);
    }

    @Transactional(readOnly = true)
    public Optional<Identity> findIdentityByUsername(String username) {
        return identityDao.loadIdentityByUsername(username);
    }

    @Transactional
    public void changePassword(Long identityId, String newPassword) {
        Identity identity = findIdentityById(identityId)
                .orElseThrow(() -> new UsernameNotFoundException("Identity not found"));

        identity.setPassword(newPassword);
        identity.setPasswordLastChanged(LocalDateTime.now());
        identityDao.updateIdentity(identity, true);
    }

    @Transactional(readOnly = true)
    public Optional<Identity> findIdentityById(Long id) {
        return identityDao.findById(id);
    }
}