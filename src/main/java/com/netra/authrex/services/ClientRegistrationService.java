package com.netra.authrex.services;

import com.netra.authrex.daos.ClientRegistrationRepository;
import com.netra.authrex.dtos.ClientRegistrationRequest;
import com.netra.authrex.exceptions.AuthenticationException;
import com.netra.authrex.exceptions.ClientRegistrationException;
import com.netra.authrex.model.ClientRegistration;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientRegistrationService {

    private final ClientRegistrationRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientRegistration registerClient(ClientRegistrationRequest request) {
        if (clientRepository.existsByClientName(request.getClientName())) {
            throw new ClientRegistrationException("Client Name already exists");
        }

        if (clientRepository.existsByClientCode(request.getClientName())) {
            throw new ClientRegistrationException("Client Name already exists");
        }

        ClientRegistration client = ClientRegistration.builder()
                .clientId(UUID.randomUUID())
                .clientSecret(passwordEncoder.encode(request.getClientSecret()))
                .clientName(request.getClientName())
                .clientCode(request.getClientCode())
                .redirectUris(new HashSet<>(request.getRedirectUris()))
                .scopes(new HashSet<>(request.getScopes()))
                .authorizedPermissions(new HashSet<>(request.getAuthorizedGrantTypes()))
                .accessTokenValidity(request.getAccessTokenValidity())
                .refreshTokenValidity(request.getRefreshTokenValidity())
                .build();

        return clientRepository.save(client);
    }

    public ClientRegistration validateClient(String clientId, String rawSecret) {
        return clientRepository.findByClientId(UUID.fromString(clientId))
                .filter(ClientRegistration::isEnabled)
                .filter(client -> passwordEncoder.matches(rawSecret, client.getClientSecret()))
                .orElseThrow(() -> new AuthenticationException("Invalid client credentials"));
    }

    public boolean clientExists(String clientId) {
        return clientRepository.existsByClientId(UUID.fromString(clientId));
    }

    public Optional<ClientRegistration> findClientById(String clientId){
        return clientRepository.findByClientId(UUID.fromString(clientId));
    }
}