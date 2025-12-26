package com.netra.authrex.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


@Entity
@Table(name = "oauth_clients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRegistration {


        @Id
        private Long id;


        private UUID clientId;

        @Column(nullable = false)
        private String clientSecret; // Store hashed!

        @Column(nullable = false)
        private String clientName;

        @Column(nullable = false)
        private String clientCode;

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "client_redirect_uris",
                joinColumns = @JoinColumn(name = "client_id"))
        @Column(name = "redirect_uri")
        private Set<String> redirectUris = new HashSet<>();

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "client_scopes",
                joinColumns = @JoinColumn(name = "client_id"))
        @Column(name = "scope")
        private Set<String> scopes = new HashSet<>();

        @ElementCollection(fetch = FetchType.EAGER)
        @CollectionTable(name = "client_grant_types",
                joinColumns = @JoinColumn(name = "client_id"))
        @Column(name = "grant_type")
        private Set<String> authorizedGrantTypes = new HashSet<>();

        @Column(nullable = false)
        private boolean enabled = true;

        private Long accessTokenValidity;
        private Long refreshTokenValidity;

        @Column(nullable = false)
        @Builder.Default
        private LocalDateTime createdAt = LocalDateTime.now();
}
