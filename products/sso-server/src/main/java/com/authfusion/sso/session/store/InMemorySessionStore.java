package com.authfusion.sso.session.store;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.session.model.SessionStatus;
import com.authfusion.sso.session.model.SsoSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ToeScope(value = "인메모리 세션 저장소", sfr = {"FIA_USB.1"})
@Component
public class InMemorySessionStore implements SessionStore {

    private final Map<String, SsoSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(SsoSession session) {
        sessions.put(session.getSessionId(), session);
    }

    @Override
    public Optional<SsoSession> findById(String sessionId) {
        SsoSession session = sessions.get(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        if (session.getExpiresAt() != null && session.getExpiresAt().isBefore(Instant.now())) {
            session.setStatus(SessionStatus.EXPIRED);
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public List<SsoSession> findByUserId(UUID userId) {
        Instant now = Instant.now();
        return sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(now))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        sessions.entrySet().removeIf(e -> e.getValue().getUserId().equals(userId));
    }

    @Override
    public long countByUserId(UUID userId) {
        return sessions.values().stream()
                .filter(s -> s.getUserId().equals(userId))
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .count();
    }

    @Override
    public List<SsoSession> findAll() {
        Instant now = Instant.now();
        return sessions.values().stream()
                .filter(s -> s.getExpiresAt() == null || s.getExpiresAt().isAfter(now))
                .collect(Collectors.toList());
    }
}
