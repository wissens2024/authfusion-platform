package com.authfusion.sso.user.service;

import com.authfusion.sso.config.GlobalExceptionHandler.AuthenticationException;
import com.authfusion.sso.config.GlobalExceptionHandler.ResourceAlreadyExistsException;
import com.authfusion.sso.config.GlobalExceptionHandler.ResourceNotFoundException;
import com.authfusion.sso.ldap.model.LdapAuthResult;
import com.authfusion.sso.ldap.service.LdapAuthenticationService;
import com.authfusion.sso.ldap.service.LdapUserSyncService;
import com.authfusion.sso.user.model.*;
import com.authfusion.sso.user.repository.PasswordHistoryRepository;
import com.authfusion.sso.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.authfusion.sso.cc.ToeScope;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service for user account management.
 * Handles user creation, retrieval, update, deletion, authentication, and password changes.
 */
@ToeScope(value = "사용자 서비스", sfr = {"FIA_UAU.1"})
@Slf4j
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordHashService passwordHashService;
    private final PasswordPolicyService passwordPolicyService;
    private final Optional<LdapAuthenticationService> ldapAuthService;
    private final Optional<LdapUserSyncService> ldapSyncService;

    @Value("${authfusion.sso.security.max-login-attempts:5}")
    private int maxLoginAttempts;

    @Value("${authfusion.sso.security.lockout-duration:1800}")
    private int lockoutDurationSeconds;

    @Autowired
    public UserService(UserRepository userRepository,
                       PasswordHistoryRepository passwordHistoryRepository,
                       PasswordHashService passwordHashService,
                       PasswordPolicyService passwordPolicyService,
                       Optional<LdapAuthenticationService> ldapAuthService,
                       Optional<LdapUserSyncService> ldapSyncService) {
        this.userRepository = userRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordHashService = passwordHashService;
        this.passwordPolicyService = passwordPolicyService;
        this.ldapAuthService = ldapAuthService;
        this.ldapSyncService = ldapSyncService;
    }

    /**
     * Creates a new user account.
     *
     * @param request user creation parameters
     * @return the created user response
     * @throws ResourceAlreadyExistsException if username or email is already taken
     */
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with username '{}'", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException(
                    "User with username '" + request.getUsername() + "' already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User with email '" + request.getEmail() + "' already exists");
        }

        // Validate password against policy
        passwordPolicyService.validate(request.getPassword());

        String hashedPassword = passwordHashService.hash(request.getPassword());

        UserEntity entity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .loginFailCount(0)
                .build();

        UserEntity saved = userRepository.save(entity);

        // Save initial password to history
        PasswordHistoryEntity historyEntry = PasswordHistoryEntity.builder()
                .userId(saved.getId())
                .passwordHash(hashedPassword)
                .build();
        passwordHistoryRepository.save(historyEntry);

        log.info("Successfully created user: id={}, username='{}'", saved.getId(), saved.getUsername());
        return UserResponse.fromEntity(saved);
    }

    /**
     * Retrieves a user by ID.
     *
     * @param id user UUID
     * @return user response
     * @throws ResourceNotFoundException if user is not found
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID id) {
        log.debug("Retrieving user with id '{}'", id);
        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(entity);
    }

    @Transactional(readOnly = true)
    public UserEntity getUserEntity(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    /**
     * Lists all users.
     *
     * @return list of all user responses
     */
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        log.debug("Listing all users");
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    /**
     * Updates an existing user account.
     * Only non-null fields in the request will be applied.
     *
     * @param id user UUID
     * @param request update parameters
     * @return updated user response
     * @throws ResourceNotFoundException if user is not found
     * @throws ResourceAlreadyExistsException if the new email is already taken
     */
    public UserResponse updateUser(UUID id, UserUpdateRequest request) {
        log.info("Updating user with id '{}'", id);

        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getEmail() != null) {
            // Check if new email is already taken by another user
            userRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResourceAlreadyExistsException(
                                "Email '" + request.getEmail() + "' is already in use");
                    });
            entity.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            entity.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            entity.setLastName(request.getLastName());
        }

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        UserEntity saved = userRepository.save(entity);
        log.info("Successfully updated user: id={}, username='{}'", saved.getId(), saved.getUsername());
        return UserResponse.fromEntity(saved);
    }

    /**
     * Deletes a user account by ID.
     *
     * @param id user UUID
     * @throws ResourceNotFoundException if user is not found
     */
    public void deleteUser(UUID id) {
        log.info("Deleting user with id '{}'", id);

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }

        userRepository.deleteById(id);
        log.info("Successfully deleted user: id={}", id);
    }

    /**
     * Changes a user's password after verifying the current password.
     * The new password is validated against policy and checked against history.
     *
     * @param id user UUID
     * @param request password change parameters
     * @throws ResourceNotFoundException if user is not found
     * @throws AuthenticationException if current password is incorrect
     */
    public void changePassword(UUID id, PasswordChangeRequest request) {
        log.info("Changing password for user with id '{}'", id);

        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Verify current password
        if (!passwordHashService.matches(request.getCurrentPassword(), entity.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        // Validate new password against policy
        passwordPolicyService.validate(request.getNewPassword());

        // Check password history
        passwordPolicyService.checkHistory(id, request.getNewPassword());

        // Hash and update password
        String newHashedPassword = passwordHashService.hash(request.getNewPassword());
        entity.setPasswordHash(newHashedPassword);
        entity.setLoginFailCount(0);
        userRepository.save(entity);

        // Save to password history
        PasswordHistoryEntity historyEntry = PasswordHistoryEntity.builder()
                .userId(id)
                .passwordHash(newHashedPassword)
                .build();
        passwordHistoryRepository.save(historyEntry);

        log.info("Successfully changed password for user: id={}, username='{}'",
                entity.getId(), entity.getUsername());
    }

    /**
     * Authenticates a user by verifying username and password.
     * Manages login failure counts and account lockout.
     *
     * @param username the username
     * @param password the plaintext password
     * @return the authenticated UserEntity
     * @throws AuthenticationException if credentials are invalid or account is locked
     */
    public UserEntity authenticate(String username, String password) {
        log.info("Authenticating user '{}'", username);

        // Try LDAP authentication first if enabled and user is not local-only
        if (ldapAuthService.isPresent() && ldapSyncService.isPresent()) {
            java.util.Optional<UserEntity> existingUser = userRepository.findByUsername(username);
            boolean isLdapUser = existingUser.map(u -> "LDAP".equals(u.getUserSource())).orElse(false);
            boolean isLocalUser = existingUser.map(u -> "LOCAL".equals(u.getUserSource())).orElse(false);

            // If user doesn't exist locally or is LDAP-sourced, try LDAP
            if (!isLocalUser) {
                LdapAuthResult ldapResult = ldapAuthService.get().authenticate(username, password);
                if (ldapResult.isSuccess()) {
                    UserEntity syncedUser = ldapSyncService.get().syncUser(ldapResult.getUserAttributes());
                    syncedUser.setLastLoginAt(LocalDateTime.now());
                    syncedUser.setLoginFailCount(0);
                    return userRepository.save(syncedUser);
                }
                // If user is LDAP-sourced but LDAP auth failed, don't fall through to local
                if (isLdapUser) {
                    throw new AuthenticationException("Invalid username or password");
                }
            }
        }

        UserEntity entity = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));

        // Check if account is locked
        if (entity.getStatus() == UserStatus.LOCKED) {
            if (entity.getLockedUntil() != null && entity.getLockedUntil().isAfter(LocalDateTime.now())) {
                throw new AuthenticationException(
                        "Account is temporarily locked. Please try again later.");
            }
            // Lock period expired, unlock the account
            entity.setStatus(UserStatus.ACTIVE);
            entity.setLoginFailCount(0);
            entity.setLockedUntil(null);
        }

        // Check if account is inactive or pending
        if (entity.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("Account is not active.");
        }

        // Verify password
        if (!passwordHashService.matches(password, entity.getPasswordHash())) {
            // Increment fail count
            int failCount = entity.getLoginFailCount() + 1;
            entity.setLoginFailCount(failCount);

            if (failCount >= maxLoginAttempts) {
                entity.setStatus(UserStatus.LOCKED);
                entity.setLockedUntil(LocalDateTime.now().plusSeconds(lockoutDurationSeconds));
                userRepository.save(entity);
                log.warn("Account locked for user '{}' after {} failed attempts",
                        username, failCount);
                throw new AuthenticationException(
                        "Account has been locked due to " + failCount + " failed login attempts");
            }

            userRepository.save(entity);
            log.warn("Failed login attempt {} of {} for user '{}'",
                    failCount, maxLoginAttempts, username);
            throw new AuthenticationException("Invalid username or password");
        }

        // Successful authentication - reset fail count and update last login
        entity.setLoginFailCount(0);
        entity.setLockedUntil(null);
        entity.setLastLoginAt(LocalDateTime.now());
        userRepository.save(entity);

        log.info("Successfully authenticated user '{}'", username);
        return entity;
    }
}
