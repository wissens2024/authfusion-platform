package com.authfusion.sso.user.controller;

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
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for authentication operations.
 */
@ToeScope(value = "사용자 인증 API", sfr = {"FIA_UAU.1", "FIA_UID.1", "FIA_AFL.1"})
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication operations (login/logout)")
public class AuthController {

    private final UserService userService;
    private final TotpService totpService;
    private final MfaSessionService mfaSessionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RoleService roleService;

    @Autowired
    public AuthController(UserService userService, TotpService totpService,
                          MfaSessionService mfaSessionService, JwtTokenProvider jwtTokenProvider,
                          RoleService roleService) {
        this.userService = userService;
        this.totpService = totpService;
        this.mfaSessionService = mfaSessionService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.roleService = roleService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with username and password. Returns a session ID and user information on success. " +
                    "Tracks failed login attempts and locks the account after exceeding the maximum allowed attempts.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Authentication successful"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials or account locked")
            }
    )
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {
        log.info("POST /api/v1/auth/login - Authenticating user '{}'", request.getUsername());

        UserEntity authenticatedUser = userService.authenticate(
                request.getUsername(), request.getPassword());

        // Check if MFA is required
        if (totpService.isTotpEnabled(authenticatedUser.getId())) {
            MfaPendingSessionEntity pending = mfaSessionService.createPendingSession(
                    authenticatedUser.getId(), null, null,
                    null, null, null, null, null, null, null, null);

            LoginResponse response = LoginResponse.builder()
                    .mfaRequired(true)
                    .mfaToken(pending.getMfaToken())
                    .build();

            log.info("MFA required for user '{}', mfaToken issued", request.getUsername());
            return ResponseEntity.ok(response);
        }

        // Generate JWT access token
        LoginResponse response = buildLoginResponse(authenticatedUser, session);

        log.info("User '{}' authenticated successfully", request.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    @Operation(
            summary = "Verify MFA code",
            description = "Verifies the TOTP code for MFA-enabled users after initial password authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "MFA verification successful"),
                    @ApiResponse(responseCode = "401", description = "Invalid MFA code or token")
            }
    )
    public ResponseEntity<LoginResponse> verifyMfa(
            @RequestParam String mfaToken,
            @Valid @RequestBody TotpVerifyRequest verifyRequest,
            HttpSession session) {
        MfaPendingSessionEntity pending = mfaSessionService.validateAndGet(mfaToken);

        boolean valid = totpService.verifyTotp(pending.getUserId(), verifyRequest.getCode());
        if (!valid) {
            throw new com.authfusion.sso.config.GlobalExceptionHandler.AuthenticationException("Invalid TOTP code");
        }

        // MFA passed - generate JWT
        UserEntity user = userService.getUserEntity(pending.getUserId());
        mfaSessionService.consumePendingSession(mfaToken);

        LoginResponse response = buildLoginResponse(user, session);

        log.info("MFA verification successful for user '{}'", user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout user",
            description = "Invalidates the current session, effectively logging the user out.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Logout successful")
            }
    )
    public ResponseEntity<Void> logout(HttpSession session) {
        log.info("POST /api/v1/auth/logout - Invalidating session '{}'", session.getId());
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private LoginResponse buildLoginResponse(UserEntity user, HttpSession session) {
        // Get user roles
        List<String> roleNames = roleService.getUserRoles(user.getId()).stream()
                .map(r -> r.getName())
                .toList();

        // Build JWT claims
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

        // Store in session
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
}
