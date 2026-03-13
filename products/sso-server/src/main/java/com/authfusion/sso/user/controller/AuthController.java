package com.authfusion.sso.user.controller;

import com.authfusion.sso.audit.service.AuditService;
import com.authfusion.sso.config.GlobalExceptionHandler.AuthenticationException;
import com.authfusion.sso.jwt.JwtTokenProvider;
import com.authfusion.sso.jwt.TokenClaims;
import com.authfusion.sso.mfa.service.MfaSessionService;
import com.authfusion.sso.mfa.service.TotpService;
import com.authfusion.sso.mfa.model.MfaPendingSessionEntity;
import com.authfusion.sso.mfa.model.TotpVerifyRequest;
import com.authfusion.sso.rbac.service.RoleService;
import com.authfusion.sso.user.model.*;
import com.authfusion.sso.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.authfusion.sso.cc.ToeScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ToeScope(value = "사용자 인증 API", sfr = {"FIA_UAU.1", "FIA_UID.1", "FIA_AFL.1"})
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final UserService userService;
    private final TotpService totpService;
    private final MfaSessionService mfaSessionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RoleService roleService;
    private final AuditService auditService;

    @Autowired
    public AuthController(UserService userService, TotpService totpService,
                          MfaSessionService mfaSessionService, JwtTokenProvider jwtTokenProvider,
                          RoleService roleService, AuditService auditService) {
        this.userService = userService;
        this.totpService = totpService;
        this.mfaSessionService = mfaSessionService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.roleService = roleService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
            })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session,
            HttpServletRequest httpRequest) {
        log.info("POST /api/v1/auth/login - user='{}'", request.getUsername());
        String ip = getClientIp(httpRequest);

        try {
            UserEntity user = userService.authenticate(request.getUsername(), request.getPassword());

            if (totpService.isTotpEnabled(user.getId())) {
                MfaPendingSessionEntity pending = mfaSessionService.createPendingSession(
                        user.getId(), null, null, null, null, null, null, null, null, null, null);
                auditService.logAuthentication("LOGIN_MFA_REQUIRED",
                        user.getId().toString(), user.getUsername(), ip, true, null);
                return ResponseEntity.ok(LoginResponse.builder()
                        .mfaRequired(true).mfaToken(pending.getMfaToken()).build());
            }

            auditService.logAuthentication("LOGIN_SUCCESS",
                    user.getId().toString(), user.getUsername(), ip, true, null);
            log.info("User '{}' authenticated successfully", request.getUsername());
            return ResponseEntity.ok(buildLoginResponse(user, session));

        } catch (AuthenticationException e) {
            auditService.logAuthentication("LOGIN_FAILED",
                    request.getUsername(), request.getUsername(), ip, false, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA code",
            responses = {
                    @ApiResponse(responseCode = "200", description = "MFA verification successful"),
                    @ApiResponse(responseCode = "401", description = "Invalid MFA code")
            })
    public ResponseEntity<LoginResponse> verifyMfa(
            @RequestParam String mfaToken,
            @Valid @RequestBody TotpVerifyRequest verifyRequest,
            HttpSession session,
            HttpServletRequest httpRequest) {
        MfaPendingSessionEntity pending = mfaSessionService.validateAndGet(mfaToken);
        String ip = getClientIp(httpRequest);

        boolean valid = totpService.verifyTotp(pending.getUserId(), verifyRequest.getCode());
        if (!valid) {
            auditService.logAuthentication("MFA_FAILED",
                    pending.getUserId().toString(), null, ip, false, "Invalid TOTP code");
            throw new AuthenticationException("Invalid TOTP code");
        }

        UserEntity user = userService.getUserEntity(pending.getUserId());
        mfaSessionService.consumePendingSession(mfaToken);
        auditService.logAuthentication("MFA_SUCCESS",
                user.getId().toString(), user.getUsername(), ip, true, null);

        log.info("MFA verification successful for user '{}'", user.getUsername());
        return ResponseEntity.ok(buildLoginResponse(user, session));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user",
            responses = {@ApiResponse(responseCode = "204", description = "Logout successful")})
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private LoginResponse buildLoginResponse(UserEntity user, HttpSession session) {
        List<String> roleNames = roleService.getUserRoles(user.getId()).stream()
                .map(r -> r.getName()).toList();

        TokenClaims claims = TokenClaims.builder()
                .sub(user.getId().toString())
                .preferredUsername(user.getUsername())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .givenName(user.getFirstName())
                .familyName(user.getLastName())
                .roles(roleNames)
                .scope("openid profile email roles")
                .clientId("admin-console")
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(claims);
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidity())
                .sessionId(session.getId())
                .user(UserResponse.fromEntity(user))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
