package com.authfusion.sso.rbac.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a user-to-role assignment.
 * Maps to the sso_user_roles table.
 */
@Entity
@Table(name = "sso_user_roles", indexes = {
        @Index(name = "idx_sso_user_roles_user_id", columnList = "user_id"),
        @Index(name = "idx_sso_user_roles_role_id", columnList = "role_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_sso_user_roles_user_role",
                columnNames = {"user_id", "role_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The UUID of the user to whom this role is assigned. */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** The UUID of the assigned role. */
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
