package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.validation.constraints.Email;
import lombok.*;

/**
 * Request DTO for updating an existing user account.
 * All fields are optional; only provided fields will be updated.
 */
@ToeScope(value = "사용자 수정 요청 모델", sfr = {"FIA_UAU.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {

    @Email(message = "Email must be a valid email address")
    private String email;

    private String firstName;

    private String lastName;

    private UserStatus status;
}
