package com.authfusion.sso.audit.controller;

import com.authfusion.sso.audit.model.AuditStatisticsResponse;
import com.authfusion.sso.audit.service.AuditService;
import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ExtendedFeature("감사 통계/분석 API")
@ConditionalOnExtendedMode
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit Statistics")
@RequiredArgsConstructor
@Slf4j
public class AuditStatisticsController {

    private final AuditService auditService;

    @GetMapping("/statistics")
    @Operation(summary = "Get audit statistics")
    public ResponseEntity<AuditStatisticsResponse> getStatistics() {
        return ResponseEntity.ok(auditService.getStatistics());
    }
}
