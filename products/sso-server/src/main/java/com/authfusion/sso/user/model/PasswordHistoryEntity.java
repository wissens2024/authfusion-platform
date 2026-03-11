package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a historical password entry.
 * Used to enforce password reuse policies.
 * Maps to the sso_password_history table.
 */
@ToeScope(value = "패스워드 이력 엔티티", sfr = {"FCS_COP.1"})
@Entity
@Table(name = "sso_password_history", indexes = {
        @Index(name = "idx_sso_password_history_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 512)
    private String passwordHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
