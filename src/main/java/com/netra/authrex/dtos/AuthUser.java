package com.netra.authrex.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.netra.commons.models.Identity;
import com.netra.commons.models.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthUser implements UserDetails {

    private final Identity identity;

    public AuthUser(Identity identity) {
        this.identity = Objects.requireNonNull(identity, "Identity must not be null");
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<Role> roles = identity.getRoles();
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toUnmodifiableSet());
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return identity.getPassword();
    }

    @Override
    public String getUsername() {
        return identity.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        // If you don’t track account expiration, just return true
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !identity.getLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        // If you do track credential expiration, you could check here
        // e.g., compare passwordLastChanged with some policy
        return true;
    }

    @Override
    public boolean isEnabled() {
        return identity.isEnabled();
    }

    public Identity getIdentity() {
        return identity;
    }
}
