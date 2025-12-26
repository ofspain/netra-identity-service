package com.netra.authrex.daos;

import com.netra.authrex.model.ClientRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRegistrationRepository
        extends JpaRepository<ClientRegistration, String> {

    Optional<ClientRegistration> findByClientIdAndEnabledTrue(String clientId);

    boolean existsByClientId(UUID clientId);

    boolean existsByClientName(String name);

    boolean existsByClientCode(String code);

    Optional<ClientRegistration> findByClientId(UUID uuid);

    Optional<ClientRegistration> findByClientCode(String code);

}
