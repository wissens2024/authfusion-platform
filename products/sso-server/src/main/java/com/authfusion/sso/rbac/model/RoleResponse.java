package com.authfusion.sso.rbac.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for role information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {

    private UUID id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Converts a RoleEntity to a RoleResponse.
     *
     * @param entity the role entity
     * @return the role response DTO
     */
    public static RoleResponse fromEntity(RoleEntity entity) {
        return RoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
