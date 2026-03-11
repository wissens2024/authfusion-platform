package com.authfusion.sso.session.model;

import com.authfusion.sso.cc.ToeScope;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@ToeScope(value = "세션 정보 모델", sfr = {"FIA_USB.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionInfo {

    private String sessionId;
    private UUID userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private String status;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;

    public static SessionInfo fromSession(SsoSession session) {
        return SessionInfo.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .username(session.getUsername())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .status(session.getStatus().name())
                .createdAt(session.getCreatedAt())
                .lastAccessedAt(session.getLastAccessedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }
}
