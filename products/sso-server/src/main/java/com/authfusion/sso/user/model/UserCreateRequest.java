package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for creating a new user account.
 */
@ToeScope(value = "사용자 생성 요청 모델", sfr = {"FIA_UAU.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String firstName;

    private String lastName;
}
