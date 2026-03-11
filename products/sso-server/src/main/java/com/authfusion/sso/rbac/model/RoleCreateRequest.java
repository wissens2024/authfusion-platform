package com.authfusion.sso.rbac.model;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request DTO for creating or updating an RBAC role.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleCreateRequest {

    @NotBlank(message = "Role name is required")
    private String name;

    private String description;
}
