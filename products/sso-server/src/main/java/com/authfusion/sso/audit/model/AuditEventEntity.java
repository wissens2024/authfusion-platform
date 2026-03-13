package com.authfusion.sso.audit.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@ToeScope(value = "감사 이벤트 엔티티", sfr = {"FAU_GEN.1"})
@Entity
@Table(name = "sso_audit_events", indexes = {
        @Index(name = "idx_sso_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_sso_audit_action", columnList = "action"),
        @Index(name = "idx_sso_audit_user_id", columnList = "user_id"),
        @Index(name = "idx_sso_audit_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 128)
    private String action;

    @Column(name = "user_id", length = 256)
    private String userId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "client_id", length = 256)
    private String clientId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id", length = 256)
    private String resourceId;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
