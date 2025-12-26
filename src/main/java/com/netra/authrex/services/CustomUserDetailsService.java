package com.netra.authrex.services;


import com.netra.authrex.dtos.AuthUser;
import com.netra.authrex.exceptions.AuthenticationException;
import com.netra.authrex.model.ClientRegistration;
import com.netra.commons.enums.DomainType;
import com.netra.commons.models.Identity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final IdentityService identityService;
    private final ClientRegistrationService clientService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            // Check if this is a service principal request
            if (username.startsWith("client:")) {
                String clientId = username.substring(7);
                return createServicePrincipal(clientId);
            }

            // Normal user lookup
            return identityService.loadUserByUsername(username);

        } catch (Exception e) {
            log.error("Failed to load user by username: {}", username, e);
            throw new UsernameNotFoundException("User not found: " + username);
        }
    }

    private UserDetails createServicePrincipal(String clientId) {
        Optional<ClientRegistration> clientRegistrationOpt = clientService.findClientById(clientId);

        if(clientRegistrationOpt.isPresent()){
            ClientRegistration client = clientRegistrationOpt.get();
            Identity identity = new Identity();
            identity.setDomainType(DomainType.SYSTEM);
            identity.setPassword(client.getClientSecret());
            identity.setDomainCode(client.getClientCode());
            identity.setUsername(client.getClientName());
            identity.setDisabled(!client.isEnabled());
            return new AuthUser(identity);
        }
        throw new AuthenticationException("No Client with ID ["+clientId+"]");
    }

    public UserDetails loadClientByClientId(String clientId) {
        return createServicePrincipal(clientId);
    }
}
