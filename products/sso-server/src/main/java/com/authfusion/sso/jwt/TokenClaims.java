package com.authfusion.sso.jwt;

import lombok.*;

import com.authfusion.sso.cc.ToeScope;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@ToeScope(value = "JWT 토큰 클레임 모델", sfr = {"FCS_COP.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenClaims {

    private String sub;
    private String iss;
    private List<String> aud;
    private Instant iat;
    private Instant exp;
    private String jti;
    private String nonce;
    private String scope;
    private String clientId;
    private String preferredUsername;
    private String email;
    private boolean emailVerified;
    private String givenName;
    private String familyName;
    private List<String> roles;
    private Map<String, Object> additionalClaims;
}
