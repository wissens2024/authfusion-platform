package com.authfusion.sso.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for client information.
 * The clientSecret field is only populated upon client creation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientResponse {

    private UUID id;
    private String clientId;
    private String clientName;
    private ClientType clientType;
    private List<String> redirectUris;
    private List<String> allowedScopes;
    private List<String> allowedGrantTypes;
    private int accessTokenValidity;
    private int refreshTokenValidity;
    private boolean enabled;
    private boolean requirePkce;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Plain-text client secret. Only returned on creation, never stored in plain text. */
    private String clientSecret;

    /**
     * Converts a ClientEntity to a ClientResponse.
     * Does not include clientSecret (must be set separately on creation).
     *
     * @param entity the client entity
     * @return the client response DTO
     */
    public static ClientResponse fromEntity(ClientEntity entity) {
        return fromEntity(entity, null);
    }

    /**
     * Converts a ClientEntity to a ClientResponse, parsing JSON array fields.
     *
     * @param entity         the client entity
     * @param redirectUris   parsed list of redirect URIs
     * @param allowedScopes  parsed list of allowed scopes
     * @param allowedGrantTypes parsed list of allowed grant types
     * @return the client response DTO
     */
    public static ClientResponse fromEntity(ClientEntity entity,
                                             List<String> redirectUris,
                                             List<String> allowedScopes,
                                             List<String> allowedGrantTypes) {
        return ClientResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientName(entity.getClientName())
                .clientType(entity.getClientType())
                .redirectUris(redirectUris)
                .allowedScopes(allowedScopes)
                .allowedGrantTypes(allowedGrantTypes)
                .accessTokenValidity(entity.getAccessTokenValidity())
                .refreshTokenValidity(entity.getRefreshTokenValidity())
                .enabled(entity.isEnabled())
                .requirePkce(entity.isRequirePkce())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Converts a ClientEntity to a ClientResponse without parsed lists.
     *
     * @param entity       the client entity
     * @param clientSecret the plain-text client secret (only on creation)
     * @return the client response DTO
     */
    public static ClientResponse fromEntity(ClientEntity entity, String clientSecret) {
        return ClientResponse.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .clientName(entity.getClientName())
                .clientType(entity.getClientType())
                .accessTokenValidity(entity.getAccessTokenValidity())
                .refreshTokenValidity(entity.getRefreshTokenValidity())
                .enabled(entity.isEnabled())
                .requirePkce(entity.isRequirePkce())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .clientSecret(clientSecret)
                .build();
    }
}
