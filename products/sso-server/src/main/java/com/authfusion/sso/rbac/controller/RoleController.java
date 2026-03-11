package com.authfusion.sso.rbac.controller;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.rbac.model.RoleCreateRequest;
import com.authfusion.sso.rbac.model.RoleResponse;
import com.authfusion.sso.rbac.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for RBAC role management and role assignment operations.
 */
@ExtendedFeature("RBAC 역할 관리 API")
@ConditionalOnExtendedMode
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Role Management", description = "RBAC role CRUD and user/client role assignment")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    // ---- Role CRUD ----

    @PostMapping
    @Operation(
            summary = "Create a new role",
            description = "Creates a new RBAC role with a unique name.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Role created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "409", description = "Role with the same name already exists")
            }
    )
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleCreateRequest request) {
        log.info("POST /api/v1/roles - Creating role: name='{}'", request.getName());
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List all roles",
            description = "Returns a list of all RBAC roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
            }
    )
    public ResponseEntity<List<RoleResponse>> listRoles() {
        log.info("GET /api/v1/roles - Listing all roles");
        List<RoleResponse> roles = roleService.listRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get role by ID",
            description = "Retrieves role information by its internal UUID.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role found"),
                    @ApiResponse(responseCode = "404", description = "Role not found")
            }
    )
    public ResponseEntity<RoleResponse> getRole(
            @Parameter(description = "Role UUID") @PathVariable UUID id) {
        log.info("GET /api/v1/roles/{}", id);
        RoleResponse response = roleService.getRole(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a role",
            description = "Updates an existing role's name and description.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
                    @ApiResponse(responseCode = "404", description = "Role not found"),
                    @ApiResponse(responseCode = "409", description = "Role with the same name already exists")
            }
    )
    public ResponseEntity<RoleResponse> updateRole(
            @Parameter(description = "Role UUID") @PathVariable UUID id,
            @Valid @RequestBody RoleCreateRequest request) {
        log.info("PUT /api/v1/roles/{}", id);
        RoleResponse response = roleService.updateRole(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a role",
            description = "Permanently deletes a role.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Role deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Role not found")
            }
    )
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "Role UUID") @PathVariable UUID id) {
        log.info("DELETE /api/v1/roles/{}", id);
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    // ---- User-Role Assignment ----

    @PostMapping("/assign/user/{userId}/role/{roleId}")
    @Operation(
            summary = "Assign role to user",
            description = "Assigns an RBAC role to a user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role assigned to user successfully"),
                    @ApiResponse(responseCode = "404", description = "Role not found"),
                    @ApiResponse(responseCode = "409", description = "Role is already assigned to the user")
            }
    )
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Role UUID") @PathVariable UUID roleId) {
        log.info("POST /api/v1/roles/assign/user/{}/role/{}", userId, roleId);
        roleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/assign/user/{userId}/role/{roleId}")
    @Operation(
            summary = "Remove role from user",
            description = "Removes an RBAC role assignment from a user.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Role removed from user successfully"),
                    @ApiResponse(responseCode = "404", description = "Role assignment not found")
            }
    )
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Parameter(description = "Role UUID") @PathVariable UUID roleId) {
        log.info("DELETE /api/v1/roles/assign/user/{}/role/{}", userId, roleId);
        roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    @Operation(
            summary = "Get user roles",
            description = "Retrieves all roles assigned to a user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User roles retrieved successfully")
            }
    )
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @Parameter(description = "User UUID") @PathVariable UUID userId) {
        log.info("GET /api/v1/roles/user/{}", userId);
        List<RoleResponse> roles = roleService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    // ---- Client-Role Assignment ----

    @PostMapping("/assign/client/{clientId}/role/{roleId}")
    @Operation(
            summary = "Assign role to client",
            description = "Assigns an RBAC role to a client.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Role assigned to client successfully"),
                    @ApiResponse(responseCode = "404", description = "Role not found"),
                    @ApiResponse(responseCode = "409", description = "Role is already assigned to the client")
            }
    )
    public ResponseEntity<Void> assignRoleToClient(
            @Parameter(description = "Client UUID") @PathVariable UUID clientId,
            @Parameter(description = "Role UUID") @PathVariable UUID roleId) {
        log.info("POST /api/v1/roles/assign/client/{}/role/{}", clientId, roleId);
        roleService.assignRoleToClient(clientId, roleId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/assign/client/{clientId}/role/{roleId}")
    @Operation(
            summary = "Remove role from client",
            description = "Removes an RBAC role assignment from a client.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Role removed from client successfully"),
                    @ApiResponse(responseCode = "404", description = "Role assignment not found")
            }
    )
    public ResponseEntity<Void> removeRoleFromClient(
            @Parameter(description = "Client UUID") @PathVariable UUID clientId,
            @Parameter(description = "Role UUID") @PathVariable UUID roleId) {
        log.info("DELETE /api/v1/roles/assign/client/{}/role/{}", clientId, roleId);
        roleService.removeRoleFromClient(clientId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/client/{clientId}")
    @Operation(
            summary = "Get client roles",
            description = "Retrieves all roles assigned to a client.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Client roles retrieved successfully")
            }
    )
    public ResponseEntity<List<RoleResponse>> getClientRoles(
            @Parameter(description = "Client UUID") @PathVariable UUID clientId) {
        log.info("GET /api/v1/roles/client/{}", clientId);
        List<RoleResponse> roles = roleService.getClientRoles(clientId);
        return ResponseEntity.ok(roles);
    }
}
