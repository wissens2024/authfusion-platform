package com.authfusion.sso.oidc.endpoint;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.oidc.model.OidcDiscovery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@ToeScope(value = "OIDC Discovery 엔드포인트", sfr = {"FIA_UAU.1"})
@RestController
@Tag(name = "OIDC Discovery")
public class DiscoveryEndpoint {

    @Value("${authfusion.sso.issuer}")
    private String issuer;

    @Value("${authfusion.sso.cc.extended-features-enabled:true}")
    private boolean extendedEnabled;

    @GetMapping("/.well-known/openid-configuration")
    @Operation(summary = "OIDC Discovery metadata")
    public ResponseEntity<OidcDiscovery> discovery() {
        List<String> grantTypes = new ArrayList<>();
        grantTypes.add("authorization_code");
        if (extendedEnabled) {
            grantTypes.add("client_credentials");
        }
        grantTypes.add("refresh_token");

        OidcDiscovery discovery = OidcDiscovery.builder()
                .issuer(issuer)
                .authorizationEndpoint(issuer + "/oauth2/authorize")
                .tokenEndpoint(issuer + "/oauth2/token")
                .userinfoEndpoint(issuer + "/oauth2/userinfo")
                .jwksUri(issuer + "/.well-known/jwks.json")
                .revocationEndpoint(issuer + "/oauth2/revoke")
                .endSessionEndpoint(issuer + "/oauth2/logout")
                .responseTypesSupported(List.of("code"))
                .grantTypesSupported(grantTypes)
                .subjectTypesSupported(List.of("public"))
                .idTokenSigningAlgValuesSupported(List.of("RS256"))
                .scopesSupported(List.of("openid", "profile", "email", "roles", "offline_access"))
                .tokenEndpointAuthMethodsSupported(List.of("client_secret_post", "client_secret_basic"))
                .claimsSupported(List.of(
                        "sub", "iss", "aud", "exp", "iat", "nonce",
                        "preferred_username", "email", "email_verified",
                        "given_name", "family_name", "roles"
                ))
                .codeChallengeMethodsSupported(List.of("S256", "plain"))
                .build();

        return ResponseEntity.ok(discovery);
    }
}
