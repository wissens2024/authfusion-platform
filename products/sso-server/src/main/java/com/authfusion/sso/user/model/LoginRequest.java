package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for user authentication (login).
 */
@ToeScope(value = "로그인 요청 모델", sfr = {"FIA_UAU.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
