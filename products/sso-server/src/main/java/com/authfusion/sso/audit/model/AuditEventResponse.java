package com.authfusion.sso.audit.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventResponse {

    private UUID id;
    private String eventType;
    private String action;
    private String userId;
    private String clientId;
    private String ipAddress;
    private String userAgent;
    private String resourceType;
    private String resourceId;
    private boolean success;
    private String errorMessage;
    private String details;
    private LocalDateTime timestamp;

    public static AuditEventResponse fromEntity(AuditEventEntity entity) {
        return AuditEventResponse.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .action(entity.getAction())
                .userId(entity.getUserId())
                .clientId(entity.getClientId())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .resourceType(entity.getResourceType())
                .resourceId(entity.getResourceId())
                .success(entity.isSuccess())
                .errorMessage(entity.getErrorMessage())
                .details(entity.getDetails())
                .timestamp(entity.getTimestamp())
                .build();
    }
}
