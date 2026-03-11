package com.authfusion.sso.mfa.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpVerifyRequest {

    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "\\d{6,8}", message = "TOTP code must be 6-8 digits")
    private String code;
}
