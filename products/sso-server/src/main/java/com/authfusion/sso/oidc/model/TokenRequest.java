package com.authfusion.sso.oidc.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenRequest {

    private String grantType;
    private String code;
    private String redirectUri;
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String scope;
    private String codeVerifier;
}
