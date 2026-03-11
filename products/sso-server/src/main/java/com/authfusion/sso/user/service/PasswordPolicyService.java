package com.authfusion.sso.user.service;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.user.model.PasswordHistoryEntity;
import com.authfusion.sso.user.repository.PasswordHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for enforcing password policies including length, complexity, and history checks.
 */
@ToeScope(value = "패스워드 정책 서비스", sfr = {"FIA_SOS.1"})
@Slf4j
@Service
public class PasswordPolicyService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordHashService passwordHashService;

    @Value("${authfusion.sso.security.password-min-length:8}")
    private int minLength;

    @Value("${authfusion.sso.security.password-max-length:128}")
    private int maxLength;

    @Value("${authfusion.sso.security.password-history-count:5}")
    private int historyCount;

    @Autowired
    public PasswordPolicyService(PasswordHistoryRepository passwordHistoryRepository,
                                  PasswordHashService passwordHashService) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordHashService = passwordHashService;
    }

    /**
     * Validates a password against the configured policy rules.
     * Checks minimum/maximum length and complexity requirements.
     *
     * @param password the plaintext password to validate
     * @throws IllegalArgumentException if the password does not meet policy requirements
     */
    public void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }

        if (password.length() < minLength) {
            throw new IllegalArgumentException(
                    "Password must be at least " + minLength + " characters long");
        }

        if (password.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Password must not exceed " + maxLength + " characters");
        }

        // Complexity: at least one uppercase, one lowercase, one digit, one special character
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one uppercase letter");
        }

        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one lowercase letter");
        }

        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one digit");
        }

        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            throw new IllegalArgumentException(
                    "Password must contain at least one special character");
        }
    }

    /**
     * Checks that the new password has not been used recently by the user.
     * Compares against the most recent N password hashes in the history.
     *
     * @param userId the user's ID
     * @param newPassword the plaintext new password to check
     * @throws IllegalArgumentException if the password was recently used
     */
    public void checkHistory(UUID userId, String newPassword) {
        List<PasswordHistoryEntity> history =
                passwordHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // Check only up to historyCount entries
        int limit = Math.min(history.size(), historyCount);
        for (int i = 0; i < limit; i++) {
            if (passwordHashService.matches(newPassword, history.get(i).getPasswordHash())) {
                throw new IllegalArgumentException(
                        "Password has been used recently. Please choose a different password.");
            }
        }
    }
}
