package com.authfusion.sso.mfa.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaStatusResponse {
    private UUID userId;
    private boolean totpEnabled;
    private boolean totpVerified;
    private int recoveryCodesRemaining;
    private LocalDateTime totpEnabledAt;
}
