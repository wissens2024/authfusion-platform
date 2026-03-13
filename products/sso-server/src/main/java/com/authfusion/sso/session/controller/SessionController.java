package com.authfusion.sso.session.controller;

import com.authfusion.sso.cc.ToeScope;
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

@ToeScope(value = "세션 관리 API", sfr = {"FIA_USB.1", "FTA_SSL.3"})
@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "Session Management")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    @Operation(summary = "List all active sessions")
    public ResponseEntity<List<SessionInfo>> listSessions() {
        log.info("GET /api/v1/sessions");
        return ResponseEntity.ok(sessionService.listAllSessions());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get sessions for a specific user")
    public ResponseEntity<List<SessionInfo>> getUserSessions(@PathVariable UUID userId) {
        log.info("GET /api/v1/sessions/user/{}", userId);
        return ResponseEntity.ok(sessionService.getUserSessions(userId));
    }

    @DeleteMapping("/{sessionId}")
    @Operation(summary = "Revoke a session")
    public ResponseEntity<Void> revokeSession(@PathVariable String sessionId) {
        log.info("DELETE /api/v1/sessions/{}", sessionId);
        sessionService.revokeSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Revoke all sessions for a user")
    public ResponseEntity<Void> revokeUserSessions(@PathVariable UUID userId) {
        log.info("DELETE /api/v1/sessions/user/{}", userId);
        sessionService.revokeUserSessions(userId);
        return ResponseEntity.noContent().build();
    }
}
