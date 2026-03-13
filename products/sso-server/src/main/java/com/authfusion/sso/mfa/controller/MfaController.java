package com.authfusion.sso.mfa.controller;

import com.authfusion.sso.audit.service.AuditService;
import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.mfa.model.*;
import com.authfusion.sso.mfa.service.TotpService;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@ToeScope(value = "MFA 관리 API", sfr = {"FIA_UAU.1"})
@RestController
@RequestMapping("/api/v1/mfa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MFA", description = "Multi-Factor Authentication management")
public class MfaController {

    private final TotpService totpService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @PostMapping("/totp/setup")
    @Operation(summary = "Set up TOTP", description = "Generates a TOTP secret and QR code for the user")
    public ResponseEntity<TotpSetupResponse> setupTotp(
            @RequestParam UUID userId,
            HttpServletRequest request) {
        UserEntity user = getUserOrThrow(userId);
        TotpSetupResponse response = totpService.setupTotp(userId, user.getUsername());
        auditService.logAuthentication("MFA_TOTP_SETUP", userId.toString(), user.getUsername(), getClientIp(request), true, null);
        log.info("TOTP setup initiated for userId={}", userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/totp/verify-setup")
    @Operation(summary = "Verify TOTP setup", description = "Verifies the initial TOTP code to confirm setup")
    public ResponseEntity<Void> verifySetup(
            @RequestParam UUID userId,
            @Valid @RequestBody TotpSetupRequest setupRequest,
            HttpServletRequest request) {
        UserEntity user = getUserOrThrow(userId);
        totpService.verifySetup(userId, setupRequest.getCode());
        auditService.logAuthentication("MFA_TOTP_VERIFIED", userId.toString(), user.getUsername(), getClientIp(request), true, null);
        log.info("TOTP setup verified for userId={}", userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/totp/disable")
    @Operation(summary = "Disable TOTP", description = "Disables TOTP for the user")
    public ResponseEntity<Void> disableTotp(
            @RequestParam UUID userId,
            HttpServletRequest request) {
        UserEntity user = getUserOrThrow(userId);
        totpService.disableTotp(userId);
        auditService.logAuthentication("MFA_TOTP_DISABLED", userId.toString(), user.getUsername(), getClientIp(request), true, null);
        log.info("TOTP disabled for userId={}", userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    @Operation(summary = "Get MFA status", description = "Returns the current user's MFA status")
    public ResponseEntity<MfaStatusResponse> getMfaStatus(@RequestParam UUID userId) {
        return ResponseEntity.ok(totpService.getMfaStatus(userId));
    }

    @GetMapping("/status/{userId}")
    @Operation(summary = "Get user MFA status (admin)", description = "Returns MFA status for a specific user")
    public ResponseEntity<MfaStatusResponse> getUserMfaStatus(@PathVariable UUID userId) {
        getUserOrThrow(userId);
        return ResponseEntity.ok(totpService.getMfaStatus(userId));
    }

    @PostMapping("/totp/regenerate-recovery")
    @Operation(summary = "Regenerate recovery codes", description = "Regenerates recovery codes for the user")
    public ResponseEntity<List<String>> regenerateRecoveryCodes(
            @RequestParam UUID userId,
            HttpServletRequest request) {
        UserEntity user = getUserOrThrow(userId);
        List<String> codes = totpService.regenerateRecoveryCodes(userId);
        auditService.logAuthentication("MFA_RECOVERY_REGENERATED", userId.toString(), user.getUsername(), getClientIp(request), true, null);
        return ResponseEntity.ok(codes);
    }

    private UserEntity getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new com.authfusion.sso.config.GlobalExceptionHandler
                        .ResourceNotFoundException("User not found: " + userId));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
