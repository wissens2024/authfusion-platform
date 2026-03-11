package com.authfusion.sso.mfa.service;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.config.GlobalExceptionHandler.AuthenticationException;
import com.authfusion.sso.mfa.model.MfaPendingSessionEntity;
import com.authfusion.sso.mfa.repository.MfaPendingSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@ToeScope(value = "MFA 대기 세션 서비스", sfr = {"FIA_UAU.1"})
@Service
@Slf4j
public class MfaSessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MfaPendingSessionRepository repository;

    @Value("${authfusion.sso.mfa.pending-session.timeout:300}")
    private int sessionTimeoutSeconds;

    public MfaSessionService(MfaPendingSessionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MfaPendingSessionEntity createPendingSession(UUID userId, String ipAddress, String userAgent,
                                                         String clientId, String redirectUri, String scope,
                                                         String responseType, String state, String nonce,
                                                         String codeChallenge, String codeChallengeMethod) {
        // Clean up existing pending sessions for this user
        repository.deleteByUserId(userId);

        String mfaToken = generateMfaToken();

        MfaPendingSessionEntity entity = MfaPendingSessionEntity.builder()
                .mfaToken(mfaToken)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scope(scope)
                .responseType(responseType)
                .state(state)
                .nonce(nonce)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .expiresAt(LocalDateTime.now().plusSeconds(sessionTimeoutSeconds))
                .build();

        MfaPendingSessionEntity saved = repository.save(entity);
        log.debug("Created MFA pending session for userId={}, token={}", userId, mfaToken);
        return saved;
    }

    @Transactional(readOnly = true)
    public MfaPendingSessionEntity validateAndGet(String mfaToken) {
        MfaPendingSessionEntity session = repository.findByMfaToken(mfaToken)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired MFA token"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("MFA session has expired");
        }

        return session;
    }

    @Transactional
    public void consumePendingSession(String mfaToken) {
        repository.findByMfaToken(mfaToken).ifPresent(repository::delete);
    }

    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cleanupExpiredSessions() {
        int deleted = repository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.debug("Cleaned up {} expired MFA pending sessions", deleted);
        }
    }

    private String generateMfaToken() {
        byte[] tokenBytes = new byte[32];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
