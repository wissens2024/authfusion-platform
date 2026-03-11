package com.authfusion.sso.rbac.service;

import com.authfusion.sso.config.GlobalExceptionHandler.ResourceAlreadyExistsException;
import com.authfusion.sso.config.GlobalExceptionHandler.ResourceNotFoundException;
import com.authfusion.sso.rbac.model.*;
import com.authfusion.sso.rbac.repository.ClientRoleRepository;
import com.authfusion.sso.rbac.repository.RoleRepository;
import com.authfusion.sso.rbac.repository.UserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for RBAC role management and role assignment operations.
 * Handles CRUD for roles and user/client role assignments.
 */
@Slf4j
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ClientRoleRepository clientRoleRepository;

    @Autowired
    public RoleService(RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       ClientRoleRepository clientRoleRepository) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.clientRoleRepository = clientRoleRepository;
    }

    // ---- Role CRUD ----

    /**
     * Creates a new RBAC role.
     *
     * @param request the role creation request
     * @return the created role response
     * @throws ResourceAlreadyExistsException if a role with the same name already exists
     */
    public RoleResponse createRole(RoleCreateRequest request) {
        log.info("Creating new role: name='{}'", request.getName());

        if (roleRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Role already exists with name: " + request.getName());
        }

        RoleEntity entity = RoleEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        RoleEntity saved = roleRepository.save(entity);
        log.info("Role created: id={}, name='{}'", saved.getId(), saved.getName());

        return RoleResponse.fromEntity(saved);
    }

    /**
     * Retrieves a role by its internal UUID.
     *
     * @param id the role UUID
     * @return the role response
     * @throws ResourceNotFoundException if no role with the given id exists
     */
    @Transactional(readOnly = true)
    public RoleResponse getRole(UUID id) {
        log.debug("Retrieving role by id={}", id);
        RoleEntity entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return RoleResponse.fromEntity(entity);
    }

    /**
     * Lists all RBAC roles.
     *
     * @return list of all role responses
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        log.debug("Listing all roles");
        return roleRepository.findAll().stream()
                .map(RoleResponse::fromEntity)
                .toList();
    }

    /**
     * Updates an existing role's name and description.
     *
     * @param id      the role UUID
     * @param request the updated role data
     * @return the updated role response
     * @throws ResourceNotFoundException      if no role with the given id exists
     * @throws ResourceAlreadyExistsException if another role with the requested name already exists
     */
    public RoleResponse updateRole(UUID id, RoleCreateRequest request) {
        log.info("Updating role: id={}", id);

        RoleEntity entity = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Check name uniqueness if name is being changed
        if (!entity.getName().equals(request.getName()) && roleRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Role already exists with name: " + request.getName());
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());

        RoleEntity saved = roleRepository.save(entity);
        log.info("Role updated: id={}, name='{}'", saved.getId(), saved.getName());

        return RoleResponse.fromEntity(saved);
    }

    /**
     * Deletes a role by its internal UUID.
     *
     * @param id the role UUID
     * @throws ResourceNotFoundException if no role with the given id exists
     */
    public void deleteRole(UUID id) {
        log.info("Deleting role: id={}", id);

        if (!roleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Role not found with id: " + id);
        }

        roleRepository.deleteById(id);
        log.info("Role deleted: id={}", id);
    }

    // ---- User-Role Assignment ----

    /**
     * Assigns a role to a user.
     *
     * @param userId the user UUID
     * @param roleId the role UUID
     * @throws ResourceNotFoundException      if the role does not exist
     * @throws ResourceAlreadyExistsException if the role is already assigned to the user
     */
    public void assignRoleToUser(UUID userId, UUID roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);

        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with id: " + roleId);
        }

        if (userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new ResourceAlreadyExistsException(
                    "Role " + roleId + " is already assigned to user " + userId);
        }

        UserRoleEntity assignment = UserRoleEntity.builder()
                .userId(userId)
                .roleId(roleId)
                .build();

        userRoleRepository.save(assignment);
        log.info("Role {} assigned to user {}", roleId, userId);
    }

    /**
     * Removes a role assignment from a user.
     *
     * @param userId the user UUID
     * @param roleId the role UUID
     * @throws ResourceNotFoundException if the assignment does not exist
     */
    public void removeRoleFromUser(UUID userId, UUID roleId) {
        log.info("Removing role {} from user {}", roleId, userId);

        if (!userRoleRepository.existsByUserIdAndRoleId(userId, roleId)) {
            throw new ResourceNotFoundException(
                    "Role " + roleId + " is not assigned to user " + userId);
        }

        userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        log.info("Role {} removed from user {}", roleId, userId);
    }

    /**
     * Retrieves all roles assigned to a user.
     *
     * @param userId the user UUID
     * @return list of role responses
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(UUID userId) {
        log.debug("Retrieving roles for user {}", userId);

        return userRoleRepository.findByUserId(userId).stream()
                .map(ur -> roleRepository.findById(ur.getRoleId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Role not found with id: " + ur.getRoleId())))
                .map(RoleResponse::fromEntity)
                .toList();
    }

    // ---- Client-Role Assignment ----

    /**
     * Assigns a role to a client.
     *
     * @param clientId the client UUID
     * @param roleId   the role UUID
     * @throws ResourceNotFoundException      if the role does not exist
     * @throws ResourceAlreadyExistsException if the role is already assigned to the client
     */
    public void assignRoleToClient(UUID clientId, UUID roleId) {
        log.info("Assigning role {} to client {}", roleId, clientId);

        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with id: " + roleId);
        }

        if (clientRoleRepository.existsByClientIdAndRoleId(clientId, roleId)) {
            throw new ResourceAlreadyExistsException(
                    "Role " + roleId + " is already assigned to client " + clientId);
        }

        ClientRoleEntity assignment = ClientRoleEntity.builder()
                .clientId(clientId)
                .roleId(roleId)
                .build();

        clientRoleRepository.save(assignment);
        log.info("Role {} assigned to client {}", roleId, clientId);
    }

    /**
     * Removes a role assignment from a client.
     *
     * @param clientId the client UUID
     * @param roleId   the role UUID
     * @throws ResourceNotFoundException if the assignment does not exist
     */
    public void removeRoleFromClient(UUID clientId, UUID roleId) {
        log.info("Removing role {} from client {}", roleId, clientId);

        if (!clientRoleRepository.existsByClientIdAndRoleId(clientId, roleId)) {
            throw new ResourceNotFoundException(
                    "Role " + roleId + " is not assigned to client " + clientId);
        }

        clientRoleRepository.deleteByClientIdAndRoleId(clientId, roleId);
        log.info("Role {} removed from client {}", roleId, clientId);
    }

    /**
     * Retrieves all roles assigned to a client.
     *
     * @param clientId the client UUID
     * @return list of role responses
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getClientRoles(UUID clientId) {
        log.debug("Retrieving roles for client {}", clientId);

        return clientRoleRepository.findByClientId(clientId).stream()
                .map(cr -> roleRepository.findById(cr.getRoleId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Role not found with id: " + cr.getRoleId())))
                .map(RoleResponse::fromEntity)
                .toList();
    }
}
