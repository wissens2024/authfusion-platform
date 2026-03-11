package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for changing a user's password.
 */
@ToeScope(value = "패스워드 변경 요청 모델", sfr = {"FCS_COP.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordChangeRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
