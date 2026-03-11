package com.authfusion.sso.jwt.entity;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@ToeScope(value = "JWT 서명키 엔티티", sfr = {"FCS_CKM.1", "FCS_CKM.4"})
@Entity
@Table(name = "sso_signing_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SigningKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String kid;

    @Column(nullable = false)
    @Builder.Default
    private String algorithm = "RS256";

    @Column(name = "key_size", nullable = false)
    @Builder.Default
    private int keySize = 2048;

    @Column(name = "public_key", nullable = false, columnDefinition = "TEXT")
    private String publicKey;

    @Column(name = "encrypted_private_key", nullable = false, columnDefinition = "TEXT")
    private String encryptedPrivateKey;

    @Column(nullable = false, length = 64)
    private String iv;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = false;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;
}
