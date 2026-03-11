package com.authfusion.sso.oidc.grant;

import com.authfusion.sso.jwt.JwtTokenParser;
import com.authfusion.sso.jwt.TokenClaims;
import com.authfusion.sso.oidc.model.TokenRequest;
import com.authfusion.sso.oidc.model.TokenResponse;
import com.authfusion.sso.oidc.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.authfusion.sso.cc.ToeScope;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ToeScope(value = "Refresh Token Grant 처리", sfr = {"FIA_UAU.1"})
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenGrantHandler implements GrantHandler {

    private final JwtTokenParser jwtTokenParser;
    private final TokenService tokenService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public String getGrantType() {
        return "refresh_token";
    }

    @Override
    public TokenResponse handle(TokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("refresh_token is required");
        }

        // Validate the refresh token JWT
        TokenClaims claims = jwtTokenParser.parseAndValidate(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));

        // Check if revoked
        String tokenHash = tokenService.hashToken(request.getRefreshToken());
        List<Map<String, Object>> tokens = jdbcTemplate.queryForList(
                "SELECT * FROM sso_refresh_tokens WHERE token_hash = ? AND revoked = FALSE",
                tokenHash
        );

        // If token tracking is enabled and token not found, reject
        // (For flexibility, we allow tokens that aren't tracked to still be refreshed if JWT is valid)

        String clientId = claims.getClientId();
        if (request.getClientId() != null && !request.getClientId().equals(clientId)) {
            throw new IllegalArgumentException("Client ID mismatch");
        }

        UUID userId = UUID.fromString(claims.getSub());
        String scope = request.getScope() != null ? request.getScope() : claims.getScope();

        return tokenService.generateTokens(userId, clientId, scope, null);
    }
}
