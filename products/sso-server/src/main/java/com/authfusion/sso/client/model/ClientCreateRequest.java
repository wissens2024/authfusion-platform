package com.authfusion.sso.client.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

/**
 * Request DTO for creating or updating an OAuth 2.0 client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientCreateRequest {

    @NotBlank(message = "Client name is required")
    private String clientName;

    /** Client type: CONFIDENTIAL or PUBLIC. Defaults to CONFIDENTIAL. */
    @Builder.Default
    private ClientType clientType = ClientType.CONFIDENTIAL;

    /** List of allowed redirect URIs. */
    private List<String> redirectUris;

    /** List of allowed OAuth scopes. */
    private List<String> allowedScopes;

    /** List of allowed OAuth grant types. */
    private List<String> allowedGrantTypes;

    /** Whether this client requires PKCE. Defaults to true. */
    @Builder.Default
    private boolean requirePkce = true;
}
