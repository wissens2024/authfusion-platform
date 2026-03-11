package com.authfusion.sso.session.service;

import com.authfusion.sso.config.GlobalExceptionHandler.ResourceNotFoundException;
import com.authfusion.sso.session.model.SessionInfo;
import com.authfusion.sso.session.model.SessionStatus;
import com.authfusion.sso.session.model.SsoSession;
import com.authfusion.sso.session.store.SessionStore;
import com.authfusion.sso.cc.ToeScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ToeScope(value = "세션 서비스", sfr = {"FIA_USB.1", "FTA_SSL.3"})
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionStore sessionStore;

    @Value("${authfusion.sso.session.timeout:3600}")
    private long sessionTimeout;

    @Value("${authfusion.sso.session.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    public SsoSession createSession(UUID userId, String username, String ipAddress, String userAgent) {
        long currentCount = sessionStore.countByUserId(userId);
        if (currentCount >= maxSessionsPerUser) {
            log.warn("Max sessions ({}) reached for user: {}", maxSessionsPerUser, username);
            List<SsoSession> userSessions = sessionStore.findByUserId(userId);
            if (!userSessions.isEmpty()) {
                sessionStore.delete(userSessions.get(0).getSessionId());
            }
        }

        Instant now = Instant.now();
        SsoSession session = SsoSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status(SessionStatus.ACTIVE)
                .createdAt(now)
                .lastAccessedAt(now)
                .expiresAt(now.plusSeconds(sessionTimeout))
                .build();

        sessionStore.save(session);
        log.info("Session created for user: {} (sessionId: {})", username, session.getSessionId());
        return session;
    }

    public SsoSession getSession(String sessionId) {
        return sessionStore.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }

    public SsoSession refreshSession(String sessionId) {
        SsoSession session = getSession(sessionId);
        session.setLastAccessedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(sessionTimeout));
        sessionStore.save(session);
        return session;
    }

    public List<SessionInfo> getUserSessions(UUID userId) {
        return sessionStore.findByUserId(userId).stream()
                .map(SessionInfo::fromSession)
                .collect(Collectors.toList());
    }

    public List<SessionInfo> listAllSessions() {
        return sessionStore.findByUserId(null) != null ?
                List.of() : List.of();
    }

    public void revokeSession(String sessionId) {
        SsoSession session = getSession(sessionId);
        session.setStatus(SessionStatus.REVOKED);
        sessionStore.delete(sessionId);
        log.info("Session revoked: {}", sessionId);
    }

    public void revokeUserSessions(UUID userId) {
        sessionStore.deleteByUserId(userId);
        log.info("All sessions revoked for user: {}", userId);
    }

    public boolean isSessionValid(String sessionId) {
        return sessionStore.findById(sessionId)
                .map(s -> s.getStatus() == SessionStatus.ACTIVE)
                .orElse(false);
    }
}
