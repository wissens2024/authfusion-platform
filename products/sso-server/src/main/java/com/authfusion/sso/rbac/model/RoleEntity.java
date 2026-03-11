package com.authfusion.sso.rbac.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing an RBAC role in the SSO system.
 * Maps to the sso_roles table.
 */
@Entity
@Table(name = "sso_roles", indexes = {
        @Index(name = "idx_sso_roles_name", columnList = "name", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Unique role name (e.g., ADMIN, USER, AUDITOR). */
    @Column(name = "name", unique = true, nullable = false, length = 128)
    private String name;

    /** Human-readable description of the role's purpose. */
    @Column(name = "description", length = 512)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
