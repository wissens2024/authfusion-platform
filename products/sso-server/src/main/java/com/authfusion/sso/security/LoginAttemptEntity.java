package com.authfusion.sso.security;

import com.authfusion.sso.cc.ToeScope;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@ToeScope(value = "로그인 시도 엔티티", sfr = {"FIA_AFL.1"})
@Entity
@Table(name = "sso_login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttemptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "attempted_at", nullable = false)
    private LocalDateTime attemptedAt;

    @PrePersist
    protected void onCreate() {
        if (attemptedAt == null) {
            attemptedAt = LocalDateTime.now();
        }
    }
}
