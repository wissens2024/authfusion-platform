package com.authfusion.sso.oidc.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OidcDiscovery {

    private String issuer;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("jwks_uri")
    private String jwksUri;

    @JsonProperty("revocation_endpoint")
    private String revocationEndpoint;

    @JsonProperty("end_session_endpoint")
    private String endSessionEndpoint;

    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;

    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;

    @JsonProperty("subject_types_supported")
    private List<String> subjectTypesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgValuesSupported;

    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("claims_supported")
    private List<String> claimsSupported;

    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;
}
