package com.authfusion.sso.oidc.grant;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.client.service.ClientService;
import com.authfusion.sso.oidc.model.TokenRequest;
import com.authfusion.sso.oidc.model.TokenResponse;
import com.authfusion.sso.oidc.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@ExtendedFeature("Client Credentials Grant (M2M 인증)")
@ConditionalOnExtendedMode
@Component
@RequiredArgsConstructor
@Slf4j
public class ClientCredentialsGrantHandler implements GrantHandler {

    private final ClientService clientService;
    private final TokenService tokenService;

    @Override
    public String getGrantType() {
        return "client_credentials";
    }

    @Override
    public TokenResponse handle(TokenRequest request) {
        if (request.getClientId() == null || request.getClientSecret() == null) {
            throw new IllegalArgumentException("client_id and client_secret are required");
        }

        if (!clientService.validateClientCredentials(request.getClientId(), request.getClientSecret())) {
            throw new SecurityException("Invalid client credentials");
        }

        String scope = request.getScope() != null ? request.getScope() : "";
        return tokenService.generateClientCredentialsToken(request.getClientId(), scope);
    }
}
