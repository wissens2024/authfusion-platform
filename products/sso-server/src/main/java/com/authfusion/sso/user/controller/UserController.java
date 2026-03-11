package com.authfusion.sso.user.controller;

import com.authfusion.sso.cc.ConditionalOnExtendedMode;
import com.authfusion.sso.cc.ExtendedFeature;
import com.authfusion.sso.user.model.*;
import com.authfusion.sso.user.service.UserService;
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
 * REST controller for user account management.
 */
@ExtendedFeature("사용자 관리 API")
@ConditionalOnExtendedMode
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User account CRUD operations and password management")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new user",
            description = "Creates a new user account with the provided details. Password must meet policy requirements.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "User created successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid request parameters or password policy violation"),
                    @ApiResponse(responseCode = "409", description = "Username or email already exists")
            }
    )
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        log.info("POST /api/v1/users - Creating user '{}'", request.getUsername());
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List all users",
            description = "Returns a list of all registered users.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
            }
    )
    public ResponseEntity<List<UserResponse>> listUsers() {
        log.info("GET /api/v1/users - Listing all users");
        List<UserResponse> users = userService.listUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves user information by their unique identifier.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        log.info("GET /api/v1/users/{}", id);
        UserResponse response = userService.getUser(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update user",
            description = "Updates an existing user account. Only provided fields are modified.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User updated successfully"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "409", description = "Email already in use")
            }
    )
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request) {
        log.info("PUT /api/v1/users/{}", id);
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete user",
            description = "Permanently deletes a user account. This operation is irreversible.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User UUID") @PathVariable UUID id) {
        log.info("DELETE /api/v1/users/{}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    @Operation(
            summary = "Change user password",
            description = "Changes the user's password. Requires current password verification. New password must meet policy requirements and must not have been recently used.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Password changed successfully"),
                    @ApiResponse(responseCode = "400", description = "Password policy violation or invalid current password"),
                    @ApiResponse(responseCode = "401", description = "Current password is incorrect"),
                    @ApiResponse(responseCode = "404", description = "User not found")
            }
    )
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "User UUID") @PathVariable UUID id,
            @Valid @RequestBody PasswordChangeRequest request) {
        log.info("POST /api/v1/users/{}/change-password", id);
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
