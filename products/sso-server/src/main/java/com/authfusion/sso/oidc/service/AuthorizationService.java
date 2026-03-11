package com.authfusion.sso.oidc.service;

import com.authfusion.sso.client.model.ClientEntity;
import com.authfusion.sso.client.repository.ClientRepository;
import com.authfusion.sso.oidc.model.AuthorizationRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.authfusion.sso.cc.ToeScope;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@ToeScope(value = "OIDC 인가 서비스", sfr = {"FIA_UAU.1", "FDP_ACC.1"})
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final ClientRepository clientRepository;
    private final ScopeService scopeService;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateAuthorizationCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Optional<String> validateAuthorizationRequest(AuthorizationRequest request) {
        if (!"code".equals(request.getResponseType())) {
            return Optional.of("unsupported_response_type");
        }

        if (request.getClientId() == null || request.getClientId().isBlank()) {
            return Optional.of("invalid_request: client_id is required");
        }

        Optional<ClientEntity> clientOpt = clientRepository.findByClientId(request.getClientId());
        if (clientOpt.isEmpty()) {
            return Optional.of("invalid_client");
        }

        ClientEntity client = clientOpt.get();
        if (!client.isEnabled()) {
            return Optional.of("unauthorized_client");
        }

        if (request.getRedirectUri() == null || request.getRedirectUri().isBlank()) {
            return Optional.of("invalid_request: redirect_uri is required");
        }

        List<String> redirectUris = parseJsonArray(client.getRedirectUris());
        if (!redirectUris.contains(request.getRedirectUri())) {
            return Optional.of("invalid_request: redirect_uri mismatch");
        }

        if (client.isRequirePkce() &&
                (request.getCodeChallenge() == null || request.getCodeChallenge().isBlank())) {
            return Optional.of("invalid_request: code_challenge is required (PKCE)");
        }

        return Optional.empty();
    }

    public boolean isRedirectUriValid(String clientId, String redirectUri) {
        return clientRepository.findByClientId(clientId)
                .map(client -> {
                    List<String> uris = parseJsonArray(client.getRedirectUris());
                    return uris.contains(redirectUri);
                })
                .orElse(false);
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to parse JSON array: {}", json, e);
            return List.of();
        }
    }
}
