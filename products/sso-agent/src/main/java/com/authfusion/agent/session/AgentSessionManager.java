package com.authfusion.agent.session;

import com.authfusion.agent.cc.ToeScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ToeScope(value = "Agent 세션 관리자", sfr = {"FIA_USB.1", "FTA_SSL.3"})
public class AgentSessionManager {

    private static final Logger log = LoggerFactory.getLogger(AgentSessionManager.class);

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();
    private final int sessionTimeoutSeconds;

    public AgentSessionManager(int sessionTimeoutSeconds) {
        this.sessionTimeoutSeconds = sessionTimeoutSeconds;
    }

    public AgentSession createSession(String accessToken, String refreshToken, String idToken) {
        Instant now = Instant.now();
        AgentSession session = AgentSession.builder()
                .sessionId(UUID.randomUUID().toString())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .idToken(idToken)
                .createdAt(now)
                .lastAccessedAt(now)
                .expiresAt(now.plusSeconds(sessionTimeoutSeconds))
                .build();

        sessions.put(session.getSessionId(), session);
        log.debug("Session created: {}", session.getSessionId());
        return session;
    }

    public Optional<AgentSession> getSession(String sessionId) {
        AgentSession session = sessions.get(sessionId);
        if (session == null) return Optional.empty();

        if (session.isExpired()) {
            sessions.remove(sessionId);
            return Optional.empty();
        }

        session.setLastAccessedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(sessionTimeoutSeconds));
        return Optional.of(session);
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
        log.debug("Session invalidated: {}", sessionId);
    }

    public void cleanupExpiredSessions() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(e -> e.getValue().getExpiresAt().isBefore(now));
    }
}
