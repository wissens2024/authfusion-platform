package com.authfusion.sso.rbac.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity representing a client-to-role assignment.
 * Maps to the sso_client_roles table.
 */
@Entity
@Table(name = "sso_client_roles", indexes = {
        @Index(name = "idx_sso_client_roles_client_id", columnList = "client_id"),
        @Index(name = "idx_sso_client_roles_role_id", columnList = "role_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_sso_client_roles_client_role",
                columnNames = {"client_id", "role_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The UUID of the client to which this role is assigned. */
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    /** The UUID of the assigned role. */
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
