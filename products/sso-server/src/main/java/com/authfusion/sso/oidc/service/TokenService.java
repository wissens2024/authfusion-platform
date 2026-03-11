package com.authfusion.sso.oidc.service;

import com.authfusion.sso.client.model.ClientEntity;
import com.authfusion.sso.client.repository.ClientRepository;
import com.authfusion.sso.jwt.JwtTokenProvider;
import com.authfusion.sso.jwt.TokenClaims;
import com.authfusion.sso.oidc.model.TokenResponse;
import com.authfusion.sso.rbac.model.RoleEntity;
import com.authfusion.sso.rbac.model.UserRoleEntity;
import com.authfusion.sso.rbac.repository.RoleRepository;
import com.authfusion.sso.rbac.repository.UserRoleRepository;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authfusion.sso.cc.ToeScope;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@ToeScope(value = "토큰 서비스", sfr = {"FCS_COP.1", "FIA_UAU.1"})
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public TokenResponse generateTokens(UUID userId, String clientId, String scope, String nonce) {
        Optional<UserEntity> userOpt = userRepository.findById(userId);
        Optional<ClientEntity> clientOpt = clientRepository.findByClientId(clientId);

        List<String> roles = getUserRoles(userId);

        TokenClaims.TokenClaimsBuilder claimsBuilder = TokenClaims.builder()
                .sub(userId.toString())
                .clientId(clientId)
                .scope(scope)
                .roles(roles)
                .aud(List.of(clientId));

        userOpt.ifPresent(user -> {
            claimsBuilder.preferredUsername(user.getUsername());
            claimsBuilder.email(user.getEmail());
            claimsBuilder.emailVerified(user.isEmailVerified());
            claimsBuilder.givenName(user.getFirstName());
            claimsBuilder.familyName(user.getLastName());
        });

        if (nonce != null) {
            claimsBuilder.nonce(nonce);
        }

        TokenClaims claims = claimsBuilder.build();

        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        String idToken = scope != null && scope.contains("openid")
                ? jwtTokenProvider.generateIdToken(claims)
                : null;
        String refreshToken = scope != null && scope.contains("offline_access")
                ? jwtTokenProvider.generateRefreshToken(claims)
                : jwtTokenProvider.generateRefreshToken(claims);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .refreshToken(refreshToken)
                .idToken(idToken)
                .scope(scope)
                .build();
    }

    public TokenResponse generateClientCredentialsToken(String clientId, String scope) {
        TokenClaims claims = TokenClaims.builder()
                .sub(clientId)
                .clientId(clientId)
                .scope(scope)
                .aud(List.of(clientId))
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(claims);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .scope(scope)
                .build();
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private List<String> getUserRoles(UUID userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        return userRoles.stream()
                .map(ur -> roleRepository.findById(ur.getRoleId()))
                .filter(Optional::isPresent)
                .map(opt -> opt.get().getName())
                .collect(Collectors.toList());
    }
}
