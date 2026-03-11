package com.authfusion.sso.user.model;

import com.authfusion.sso.cc.ToeScope;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user information.
 * Never exposes passwordHash in API responses.
 */
@ToeScope(value = "사용자 응답 모델", sfr = {"FIA_UAU.1"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private boolean emailVerified;
    private int loginFailCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private String userSource;
    private String externalId;
    private LocalDateTime ldapSyncedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts a UserEntity to a UserResponse, omitting the password hash.
     */
    public static UserResponse fromEntity(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .status(entity.getStatus())
                .emailVerified(entity.isEmailVerified())
                .loginFailCount(entity.getLoginFailCount())
                .lockedUntil(entity.getLockedUntil())
                .lastLoginAt(entity.getLastLoginAt())
                .userSource(entity.getUserSource())
                .externalId(entity.getExternalId())
                .ldapSyncedAt(entity.getLdapSyncedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
