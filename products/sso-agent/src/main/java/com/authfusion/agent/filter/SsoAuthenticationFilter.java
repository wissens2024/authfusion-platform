package com.authfusion.agent.filter;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.config.SsoAgentProperties;
import com.authfusion.agent.context.SsoSecurityContext;
import com.authfusion.agent.context.SsoSecurityContextHolder;
import com.authfusion.agent.session.AgentSession;
import com.authfusion.agent.session.AgentSessionManager;
import com.authfusion.agent.token.JwtTokenValidator;
import com.authfusion.agent.token.TokenInfo;
import com.authfusion.agent.util.CookieHelper;
import com.authfusion.agent.util.RedirectHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@ToeScope(value = "SSO 인증 필터", sfr = {"FIA_UAU.1", "FIA_UID.1"})
public class SsoAuthenticationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SsoAuthenticationFilter.class);
    private static final String STATE_ATTR = "sso_state";
    private static final String CODE_VERIFIER_ATTR = "sso_code_verifier";
    private static final String ORIGINAL_URL_ATTR = "sso_original_url";

    private final SsoAgentProperties properties;
    private final AgentSessionManager sessionManager;
    private final JwtTokenValidator tokenValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public SsoAuthenticationFilter(SsoAgentProperties properties,
                                    AgentSessionManager sessionManager,
                                    JwtTokenValidator tokenValidator) {
        this.properties = properties;
        this.sessionManager = sessionManager;
        this.tokenValidator = tokenValidator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestPath = httpRequest.getRequestURI();

        try {
            // Skip excluded paths
            if (isExcluded(requestPath)) {
                chain.doFilter(request, response);
                return;
            }

            // Handle SSO callback
            if (requestPath.equals(properties.getCallbackPath())) {
                handleCallback(httpRequest, httpResponse);
                return;
            }

            // Check existing session
            String sessionId = CookieHelper.getCookieValue(httpRequest, properties.getSessionCookieName());
            if (sessionId != null) {
                Optional<AgentSession> sessionOpt = sessionManager.getSession(sessionId);
                if (sessionOpt.isPresent()) {
                    AgentSession session = sessionOpt.get();
                    Optional<TokenInfo> tokenInfo = tokenValidator.validate(session.getAccessToken());
                    if (tokenInfo.isPresent()) {
                        setSecurityContext(tokenInfo.get(), session.getAccessToken(), sessionId);
                        chain.doFilter(request, response);
                        return;
                    }
                }
            }

            // No valid session - redirect to SSO Server
            redirectToSsoServer(httpRequest, httpResponse);
        } finally {
            SsoSecurityContextHolder.clearContext();
        }
    }

    private void handleCallback(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String code = request.getParameter("code");
        String state = request.getParameter("state");
        String error = request.getParameter("error");

        if (error != null) {
            log.error("SSO authorization error: {}", error);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "SSO authorization failed: " + error);
            return;
        }

        if (code == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing authorization code");
            return;
        }

        // Exchange code for tokens
        String codeVerifier = (String) request.getSession().getAttribute(CODE_VERIFIER_ATTR);
        String redirectUri = RedirectHelper.getCallbackUrl(request, properties.getCallbackPath());

        String tokenBody = "grant_type=authorization_code" +
                "&code=" + RedirectHelper.encode(code) +
                "&redirect_uri=" + RedirectHelper.encode(redirectUri) +
                "&client_id=" + RedirectHelper.encode(properties.getClientId()) +
                "&client_secret=" + RedirectHelper.encode(properties.getClientSecret()) +
                (codeVerifier != null ? "&code_verifier=" + RedirectHelper.encode(codeVerifier) : "");

        try {
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getSsoServerUrl() + "/oauth2/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (tokenResponse.statusCode() != 200) {
                log.error("Token exchange failed: {}", tokenResponse.body());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token exchange failed");
                return;
            }

            JsonNode json = objectMapper.readTree(tokenResponse.body());
            String accessToken = json.get("access_token").asText();
            String refreshToken = json.has("refresh_token") ? json.get("refresh_token").asText() : null;
            String idToken = json.has("id_token") ? json.get("id_token").asText() : null;

            AgentSession session = sessionManager.createSession(accessToken, refreshToken, idToken);

            CookieHelper.setCookie(response, properties.getSessionCookieName(),
                    session.getSessionId(), properties.getSessionTimeout(),
                    true, properties.isRequireHttps(), "/");

            // Redirect to original URL
            String originalUrl = (String) request.getSession().getAttribute(ORIGINAL_URL_ATTR);
            request.getSession().removeAttribute(STATE_ATTR);
            request.getSession().removeAttribute(CODE_VERIFIER_ATTR);
            request.getSession().removeAttribute(ORIGINAL_URL_ATTR);

            response.sendRedirect(originalUrl != null ? originalUrl : "/");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token exchange interrupted");
        }
    }

    private void redirectToSsoServer(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String state = RedirectHelper.generateState();
        String codeVerifier = RedirectHelper.generateCodeVerifier();
        String redirectUri = RedirectHelper.getCallbackUrl(request, properties.getCallbackPath());
        String originalUrl = request.getRequestURL().toString();
        if (request.getQueryString() != null) {
            originalUrl += "?" + request.getQueryString();
        }

        request.getSession().setAttribute(STATE_ATTR, state);
        request.getSession().setAttribute(CODE_VERIFIER_ATTR, codeVerifier);
        request.getSession().setAttribute(ORIGINAL_URL_ATTR, originalUrl);

        String authUrl = RedirectHelper.buildAuthorizationUrl(
                properties.getSsoServerUrl(), properties.getClientId(),
                redirectUri, properties.getScope(), state, codeVerifier);

        response.sendRedirect(authUrl);
    }

    private void setSecurityContext(TokenInfo tokenInfo, String accessToken, String sessionId) {
        SsoSecurityContext context = SsoSecurityContext.builder()
                .userId(tokenInfo.getSubject())
                .username(tokenInfo.getPreferredUsername())
                .email(tokenInfo.getEmail())
                .givenName(tokenInfo.getGivenName())
                .familyName(tokenInfo.getFamilyName())
                .roles(tokenInfo.getRoles())
                .accessToken(accessToken)
                .tokenExpiry(tokenInfo.getExpiresAt())
                .sessionId(sessionId)
                .build();
        SsoSecurityContextHolder.setContext(context);
    }

    private boolean isExcluded(String path) {
        return properties.getExcludedPaths().stream().anyMatch(path::startsWith);
    }
}
