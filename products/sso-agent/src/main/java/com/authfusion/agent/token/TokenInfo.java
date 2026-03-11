package com.authfusion.agent.token;

import com.authfusion.agent.cc.ToeScope;
import lombok.*;

import java.time.Instant;
import java.util.List;

@ToeScope(value = "토큰 정보 모델", sfr = {"FCS_COP.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenInfo {

    private String subject;
    private String issuer;
    private List<String> audience;
    private Instant issuedAt;
    private Instant expiresAt;
    private String jwtId;
    private String scope;
    private String clientId;
    private String preferredUsername;
    private String email;
    private boolean emailVerified;
    private String givenName;
    private String familyName;
    private List<String> roles;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
