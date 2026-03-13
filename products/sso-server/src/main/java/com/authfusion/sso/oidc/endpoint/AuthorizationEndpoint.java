package com.authfusion.sso.oidc.endpoint;

import com.authfusion.sso.oidc.model.AuthorizationRequest;
import com.authfusion.sso.oidc.service.AuthorizationService;
import com.authfusion.sso.session.model.SsoSession;
import com.authfusion.sso.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import com.authfusion.sso.cc.ToeScope;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@ToeScope(value = "OIDC 인가 엔드포인트", sfr = {"FIA_UAU.1", "FIA_UID.1"})
@Controller
@Tag(name = "OIDC Authorization")
@RequiredArgsConstructor
@Slf4j
public class AuthorizationEndpoint {

    private final AuthorizationService authorizationService;
    private final SessionService sessionService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${authfusion.sso.jwt.authorization-code-validity:600}")
    private long codeValidity;

    @GetMapping("/oauth2/authorize")
    @Operation(summary = "Authorization endpoint")
    public Object authorize(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", defaultValue = "openid") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false, defaultValue = "S256") String codeChallengeMethod,
            @RequestParam(value = "prompt", required = false) String prompt,
            HttpServletRequest httpRequest) {

        AuthorizationRequest request = AuthorizationRequest.builder()
                .responseType(responseType)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .state(state)
                .nonce(nonce)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .prompt(prompt)
                .build();

        Optional<String> validationError = authorizationService.validateAuthorizationRequest(request);
        if (validationError.isPresent()) {
            String error = validationError.get();
            String errorCode = error.contains(":") ? error.substring(0, error.indexOf(":")).trim() : error;
            String errorDesc = error.contains(":") ? error.substring(error.indexOf(":") + 1).trim() : error;
            return new RedirectView(redirectUri + "?error=" + encode(errorCode) +
                    "&error_description=" + encode(errorDesc) +
                    (state != null ? "&state=" + encode(state) : ""));
        }

        // Check if user has active session
        String sessionId = getSessionIdFromCookie(httpRequest);
        if (sessionId != null && sessionService.isSessionValid(sessionId)) {
            SsoSession session = sessionService.getSession(sessionId);
            return issueAuthorizationCode(session, request, redirectUri, state);
        }

        // Redirect to login page with authorization parameters
        String loginUrl = "/sso/login?" +
                "client_id=" + encode(clientId) +
                "&redirect_uri=" + encode(redirectUri) +
                "&scope=" + encode(scope) +
                "&response_type=" + encode(responseType) +
                (state != null ? "&state=" + encode(state) : "") +
                (nonce != null ? "&nonce=" + encode(nonce) : "") +
                (codeChallenge != null ? "&code_challenge=" + encode(codeChallenge) : "") +
                (codeChallengeMethod != null ? "&code_challenge_method=" + encode(codeChallengeMethod) : "");

        return new RedirectView(loginUrl);
    }

    @PostMapping("/oauth2/authorize")
    public Object authorizePost(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam(value = "scope", defaultValue = "openid") String scope,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false, defaultValue = "S256") String codeChallengeMethod,
            HttpServletRequest httpRequest) {

        return authorize(responseType, clientId, redirectUri, scope, state, nonce,
                codeChallenge, codeChallengeMethod, null, httpRequest);
    }

    private RedirectView issueAuthorizationCode(SsoSession session, AuthorizationRequest request,
                                                 String redirectUri, String state) {
        String code = authorizationService.generateAuthorizationCode();

        jdbcTemplate.update(
                "INSERT INTO sso_authorization_codes (code, client_id, user_id, redirect_uri, scope, " +
                        "code_challenge, code_challenge_method, nonce, state, expires_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                code, request.getClientId(), session.getUserId(), redirectUri, request.getScope(),
                request.getCodeChallenge(), request.getCodeChallengeMethod(),
                request.getNonce(), state, LocalDateTime.now().plusSeconds(codeValidity)
        );

        String url = redirectUri + "?code=" + encode(code) +
                (state != null ? "&state=" + encode(state) : "");
        return new RedirectView(url);
    }

    private String getSessionIdFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("SSO_SESSION".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
