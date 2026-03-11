package com.authfusion.sso.mfa.model;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpSetupResponse {
    private String secret;
    private String qrCodeDataUri;
    private String otpauthUri;
    private List<String> recoveryCodes;
}
