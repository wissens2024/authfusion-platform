package com.authfusion.sso.client.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an OAuth 2.0 / OIDC client registered in the SSO system.
 * Maps to the sso_clients table.
 */
@Entity
@Table(name = "sso_clients", indexes = {
        @Index(name = "idx_sso_clients_client_id", columnList = "client_id", unique = true),
        @Index(name = "idx_sso_clients_client_name", columnList = "client_name"),
        @Index(name = "idx_sso_clients_enabled", columnList = "enabled")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Unique OAuth 2.0 client_id issued upon registration. */
    @Column(name = "client_id", unique = true, nullable = false, length = 128)
    private String clientId;

    /** BCrypt-hashed client secret (null for PUBLIC clients). */
    @Column(name = "client_secret_hash", length = 512)
    private String clientSecretHash;

    /** Human-readable display name for the client. */
    @Column(name = "client_name", nullable = false, length = 255)
    private String clientName;

    /** Client type: CONFIDENTIAL or PUBLIC. */
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", nullable = false, length = 32)
    @Builder.Default
    private ClientType clientType = ClientType.CONFIDENTIAL;

    /** JSON array of allowed redirect URIs. */
    @Column(name = "redirect_uris", columnDefinition = "TEXT")
    private String redirectUris;

    /** JSON array of allowed OAuth scopes. */
    @Column(name = "allowed_scopes", columnDefinition = "TEXT")
    private String allowedScopes;

    /** JSON array of allowed OAuth grant types. */
    @Column(name = "allowed_grant_types", columnDefinition = "TEXT")
    private String allowedGrantTypes;

    /** Access token validity in seconds. */
    @Column(name = "access_token_validity", nullable = false)
    @Builder.Default
    private int accessTokenValidity = 3600;

    /** Refresh token validity in seconds. */
    @Column(name = "refresh_token_validity", nullable = false)
    @Builder.Default
    private int refreshTokenValidity = 86400;

    /** Whether this client is enabled for authentication. */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    /** Whether this client requires PKCE for authorization code flow. */
    @Column(name = "require_pkce", nullable = false)
    @Builder.Default
    private boolean requirePkce = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
