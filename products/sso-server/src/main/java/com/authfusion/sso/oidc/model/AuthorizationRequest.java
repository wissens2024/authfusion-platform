package com.authfusion.sso.oidc.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizationRequest {

    private String responseType;
    private String clientId;
    private String redirectUri;
    private String scope;
    private String state;
    private String nonce;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String prompt;
}
