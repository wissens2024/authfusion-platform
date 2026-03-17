package com.authfusion.sso.oidc.grant;

import com.authfusion.sso.oidc.model.TokenRequest;
import com.authfusion.sso.oidc.model.TokenResponse;
import com.authfusion.sso.oidc.service.PkceValidator;
import com.authfusion.sso.oidc.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.authfusion.sso.cc.ToeScope;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ToeScope(value = "Authorization Code Grant 처리", sfr = {"FIA_UAU.1"})
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationCodeGrantHandler implements GrantHandler {

    private final JdbcTemplate jdbcTemplate;
    private final PkceValidator pkceValidator;
    private final TokenService tokenService;

    @Override
    public String getGrantType() {
        return "authorization_code";
    }

    @Override
    public TokenResponse handle(TokenRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Authorization code is required");
        }

        List<Map<String, Object>> codes = jdbcTemplate.queryForList(
                "SELECT * FROM sso_authorization_codes WHERE code = ? AND used = FALSE",
                request.getCode()
        );

        if (codes.isEmpty()) {
            throw new IllegalArgumentException("Invalid or expired authorization code");
        }

        Map<String, Object> authCode = codes.get(0);

        LocalDateTime expiresAt = (LocalDateTime) authCode.get("expires_at");
        if (expiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Authorization code has expired");
        }

        String storedClientId = (String) authCode.get("client_id");
        if (!storedClientId.equals(request.getClientId())) {
            throw new IllegalArgumentException("Client ID mismatch");
        }

        String storedRedirectUri = (String) authCode.get("redirect_uri");
        if (!storedRedirectUri.equals(request.getRedirectUri())) {
            throw new IllegalArgumentException("Redirect URI mismatch");
        }

        // PKCE validation
        String codeChallenge = (String) authCode.get("code_challenge");
        String codeChallengeMethod = (String) authCode.get("code_challenge_method");
        if (codeChallenge != null) {
            if (request.getCodeVerifier() == null) {
                throw new IllegalArgumentException("code_verifier is required for PKCE");
            }
            if (!pkceValidator.validate(request.getCodeVerifier(), codeChallenge, codeChallengeMethod)) {
                throw new IllegalArgumentException("PKCE validation failed");
            }
        }

        // Mark code as used atomically to prevent reuse race condition
        int updated = jdbcTemplate.update(
                "UPDATE sso_authorization_codes SET used = TRUE WHERE code = ? AND used = FALSE",
                request.getCode());
        if (updated == 0) {
            throw new IllegalArgumentException("Authorization code already used or expired");
        }

        UUID userId = (UUID) authCode.get("user_id");
        String scope = (String) authCode.get("scope");
        String nonce = (String) authCode.get("nonce");

        return tokenService.generateTokens(userId, storedClientId, scope, nonce);
    }
}
