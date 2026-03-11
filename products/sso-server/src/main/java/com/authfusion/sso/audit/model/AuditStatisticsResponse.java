package com.authfusion.sso.audit.model;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditStatisticsResponse {

    private long totalEvents;
    private long successfulEvents;
    private long failedEvents;
    private Map<String, Long> eventsByType;
    private Map<String, Long> eventsByAction;
}
