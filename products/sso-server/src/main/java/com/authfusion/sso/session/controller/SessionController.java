package com.authfusion.sso.session.controller;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.session.model.SessionInfo;
import com.authfusion.sso.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@ExtendedFeature("세션 관리 API")
@ConditionalOnExtendedMode
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Session Management")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user sessions")
    public ResponseEntity<List<SessionInfo>> getUserSessions(@PathVariable UUID userId) {
        return ResponseEntity.ok(sessionService.getUserSessions(userId));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a session")
    public ResponseEntity<Void> revokeSession(@PathVariable String sessionId) {
        sessionService.revokeSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Revoke all user sessions")
    public ResponseEntity<Void> revokeUserSessions(@PathVariable UUID userId) {
        sessionService.revokeUserSessions(userId);
        return ResponseEntity.noContent().build();
    }
}
