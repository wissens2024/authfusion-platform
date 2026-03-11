package com.authfusion.agent.session;

import com.authfusion.agent.cc.ToeScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ToeScope(value = "세션 동기화 서비스", sfr = {"FIA_USB.1"})
public class SessionSyncService {

    private static final Logger log = LoggerFactory.getLogger(SessionSyncService.class);

    private final String ssoServerUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SessionSyncService(String ssoServerUrl) {
        this.ssoServerUrl = ssoServerUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isSessionActive(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ssoServerUrl + "/oauth2/userinfo"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to check session with SSO server", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    public String refreshAccessToken(String refreshToken, String clientId, String clientSecret) {
        try {
            String body = "grant_type=refresh_token" +
                    "&refresh_token=" + refreshToken +
                    "&client_id=" + clientId +
                    "&client_secret=" + clientSecret;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ssoServerUrl + "/oauth2/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                return json.get("access_token").asText();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to refresh access token", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }
}
