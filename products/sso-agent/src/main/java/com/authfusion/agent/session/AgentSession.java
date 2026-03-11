package com.authfusion.agent.session;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.token.TokenInfo;
import lombok.*;

import java.time.Instant;

@ToeScope(value = "Agent 세션 모델", sfr = {"FIA_USB.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSession {

    private String sessionId;
    private String accessToken;
    private String refreshToken;
    private String idToken;
    private TokenInfo tokenInfo;
    private Instant createdAt;
    private Instant lastAccessedAt;
    private Instant expiresAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public boolean isTokenExpired() {
        return tokenInfo != null && tokenInfo.isExpired();
    }
}
