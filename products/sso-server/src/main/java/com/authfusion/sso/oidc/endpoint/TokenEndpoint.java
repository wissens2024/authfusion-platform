package com.authfusion.sso.oidc.endpoint;

import com.authfusion.sso.oidc.grant.GrantHandler;
import com.authfusion.sso.oidc.model.OidcError;
import com.authfusion.sso.oidc.model.TokenRequest;
import com.authfusion.sso.oidc.model.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.authfusion.sso.cc.ToeScope;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ToeScope(value = "토큰 엔드포인트", sfr = {"FIA_UAU.1"})
@RestController
@Tag(name = "OIDC Token")
@Slf4j
public class TokenEndpoint {

    private final Map<String, GrantHandler> grantHandlers;

    public TokenEndpoint(List<GrantHandler> handlers) {
        this.grantHandlers = handlers.stream()
                .collect(Collectors.toMap(GrantHandler::getGrantType, Function.identity()));
    }

    @PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "Token endpoint")
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "client_secret", required = false) String clientSecret,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier) {

        GrantHandler handler = grantHandlers.get(grantType);
        if (handler == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(OidcError.builder()
                            .error(OidcError.UNSUPPORTED_GRANT_TYPE)
                            .errorDescription("Unsupported grant type: " + grantType)
                            .build());
        }

        try {
            TokenRequest request = TokenRequest.builder()
                    .grantType(grantType)
                    .code(code)
                    .redirectUri(redirectUri)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .refreshToken(refreshToken)
                    .scope(scope)
                    .codeVerifier(codeVerifier)
                    .build();

            TokenResponse response = handler.handle(request);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.warn("Token request failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(OidcError.builder()
                            .error(OidcError.INVALID_CLIENT)
                            .errorDescription(e.getMessage())
                            .build());
        } catch (IllegalArgumentException e) {
            log.warn("Token request invalid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(OidcError.builder()
                            .error(OidcError.INVALID_GRANT)
                            .errorDescription(e.getMessage())
                            .build());
        } catch (Exception e) {
            log.error("Token endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(OidcError.builder()
                            .error(OidcError.SERVER_ERROR)
                            .errorDescription("Internal server error")
                            .build());
        }
    }
}
