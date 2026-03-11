package com.authfusion.sso.session.model;

import com.authfusion.sso.cc.ToeScope;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@ToeScope(value = "SSO 세션 모델", sfr = {"FIA_USB.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoSession {

    private String sessionId;
    private UUID userId;
    private String username;
    private String ipAddress;
    private String userAgent;
    private SessionStatus status;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;
}
