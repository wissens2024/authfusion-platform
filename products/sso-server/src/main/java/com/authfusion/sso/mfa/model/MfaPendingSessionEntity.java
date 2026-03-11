package com.authfusion.sso.mfa.model;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@ToeScope(value = "MFA 대기 세션 엔티티", sfr = {"FIA_UAU.1"})
@Entity
@Table(name = "sso_mfa_pending_sessions")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaPendingSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "mfa_token", nullable = false, unique = true, length = 512)
    private String mfaToken;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "client_id", length = 255)
    private String clientId;

    @Column(name = "redirect_uri", columnDefinition = "TEXT")
    private String redirectUri;

    @Column(name = "scope", length = 512)
    private String scope;

    @Column(name = "response_type", length = 64)
    private String responseType;

    @Column(name = "state", length = 512)
    private String state;

    @Column(name = "nonce", length = 512)
    private String nonce;

    @Column(name = "code_challenge", length = 512)
    private String codeChallenge;

    @Column(name = "code_challenge_method", length = 16)
    private String codeChallengeMethod;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
