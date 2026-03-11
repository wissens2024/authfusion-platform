package com.authfusion.sso.audit.controller;

import com.authfusion.sso.audit.model.AuditEventResponse;
import com.authfusion.sso.audit.service.AuditService;
import com.authfusion.sso.cc.ToeScope;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@ToeScope(value = "감사 로그 조회 API", sfr = {"FAU_GEN.1", "FAU_SAR.1"})
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit Events")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/events")
    @Operation(summary = "Query audit events")
    public ResponseEntity<Page<AuditEventResponse>> getEvents(
            @Parameter(description = "Filter by event type")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by user ID")
            @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by action")
            @RequestParam(required = false) String action,
            @Parameter(description = "Filter by success status")
            @RequestParam(required = false) Boolean success,
            @Parameter(description = "From timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "To timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditService.getEvents(eventType, userId, action, success, from, to, page, size));
    }
}
