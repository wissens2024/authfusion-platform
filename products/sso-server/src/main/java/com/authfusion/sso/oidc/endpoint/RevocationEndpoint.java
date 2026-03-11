package com.authfusion.sso.oidc.endpoint;

import com.authfusion.sso.jwt.JwtTokenParser;
import com.authfusion.sso.jwt.TokenClaims;
import com.authfusion.sso.oidc.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.authfusion.sso.cc.ToeScope;

import java.util.Optional;

@ToeScope(value = "토큰 폐기 엔드포인트", sfr = {"FIA_UAU.1"})
@RestController
@Tag(name = "OIDC Token Revocation")
@RequiredArgsConstructor
@Slf4j
public class RevocationEndpoint {

    private final JwtTokenParser jwtTokenParser;
    private final TokenService tokenService;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping(value = "/oauth2/revoke", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Revoke a token")
    public ResponseEntity<Void> revoke(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {

        Optional<TokenClaims> claimsOpt = jwtTokenParser.parseAndValidate(token);
        if (claimsOpt.isEmpty()) {
            // Per RFC 7009, return 200 even if token is invalid
            return ResponseEntity.ok().build();
        }

        // If it's a refresh token, mark as revoked
        String tokenHash = tokenService.hashToken(token);
        jdbcTemplate.update(
                "UPDATE sso_refresh_tokens SET revoked = TRUE WHERE token_hash = ?",
                tokenHash
        );

        log.info("Token revoked for subject: {}", claimsOpt.get().getSub());
        return ResponseEntity.ok().build();
    }
}
