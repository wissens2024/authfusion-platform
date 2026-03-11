package com.authfusion.sso.mfa.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@ToeScope(value = "TOTP 비밀키 엔티티", sfr = {"FIA_UAU.1", "FCS_COP.1"})
@Entity
@Table(name = "sso_totp_secrets")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TotpSecretEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "encrypted_secret", nullable = false, columnDefinition = "TEXT")
    private String encryptedSecret;

    @Column(name = "iv", nullable = false, length = 64)
    private String iv;

    @Column(name = "algorithm", nullable = false, length = 16)
    @Builder.Default
    private String algorithm = "HmacSHA1";

    @Column(name = "digits", nullable = false)
    @Builder.Default
    private int digits = 6;

    @Column(name = "period", nullable = false)
    @Builder.Default
    private int period = 30;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
