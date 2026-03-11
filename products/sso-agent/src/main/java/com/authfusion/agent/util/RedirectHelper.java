package com.authfusion.agent.util;

import com.authfusion.agent.cc.ToeScope;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@ToeScope(value = "리다이렉트 헬퍼", sfr = {})
public final class RedirectHelper {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RedirectHelper() {}

    public static String buildAuthorizationUrl(String ssoServerUrl, String clientId,
                                                String redirectUri, String scope,
                                                String state, String codeVerifier) {
        String codeChallenge = generateCodeChallenge(codeVerifier);

        return ssoServerUrl + "/oauth2/authorize?" +
                "response_type=code" +
                "&client_id=" + encode(clientId) +
                "&redirect_uri=" + encode(redirectUri) +
                "&scope=" + encode(scope) +
                "&state=" + encode(state) +
                "&code_challenge=" + encode(codeChallenge) +
                "&code_challenge_method=S256";
    }

    public static String getCallbackUrl(HttpServletRequest request, String callbackPath) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (("http".equals(scheme) && serverPort != 80) ||
                ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath).append(callbackPath);
        return url.toString();
    }

    public static String generateState() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
