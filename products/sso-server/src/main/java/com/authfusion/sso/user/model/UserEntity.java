package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a user account in the SSO system.
 * Maps to the sso_users table.
 */
@ToeScope(value = "사용자 엔티티", sfr = {"FIA_UAU.1"})
@Entity
@Table(name = "sso_users", indexes = {
        @Index(name = "idx_sso_users_username", columnList = "username", unique = true),
        @Index(name = "idx_sso_users_email", columnList = "email", unique = true),
        @Index(name = "idx_sso_users_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", unique = true, nullable = false, length = 255)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "login_fail_count", nullable = false)
    @Builder.Default
    private int loginFailCount = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "user_source", nullable = false, length = 16)
    @Builder.Default
    private String userSource = "LOCAL";

    @Column(name = "external_id", length = 512)
    private String externalId;

    @Column(name = "ldap_synced_at")
    private LocalDateTime ldapSyncedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
