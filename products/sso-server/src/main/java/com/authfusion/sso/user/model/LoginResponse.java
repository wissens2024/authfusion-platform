package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import lombok.*;

/**
 * Response DTO for successful authentication, containing session ID and user information.
 */
@ToeScope(value = "로그인 응답 모델", sfr = {"FIA_UAU.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String sessionId;
    private UserResponse user;
    private boolean mfaRequired;
    private String mfaToken;
}
