package com.authfusion.agent.context;

import com.authfusion.agent.cc.ToeScope;
import lombok.*;

import java.time.Instant;
import java.util.List;

@ToeScope(value = "SSO 보안 컨텍스트", sfr = {"FIA_USB.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SsoSecurityContext {

    private String userId;
    private String username;
    private String email;
    private boolean emailVerified;
    private String givenName;
    private String familyName;
    private List<String> roles;
    private String accessToken;
    private Instant tokenExpiry;
    private String sessionId;

    public boolean isAuthenticated() {
        return userId != null && accessToken != null;
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean hasAnyRole(String... checkRoles) {
        if (roles == null) return false;
        for (String role : checkRoles) {
            if (roles.contains(role)) return true;
        }
        return false;
    }

    public boolean isTokenExpired() {
        return tokenExpiry != null && Instant.now().isAfter(tokenExpiry);
    }
}
