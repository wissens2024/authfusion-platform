package com.authfusion.sso.user.service;

import lombok.extern.slf4j.Slf4j;
import com.authfusion.sso.cc.ToeScope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service for password hashing and verification using BCrypt.
 * Wraps Spring Security's PasswordEncoder for consistent usage across the application.
 */
@ToeScope(value = "패스워드 해시 서비스", sfr = {"FCS_COP.1"})
@Slf4j
@Service
public class PasswordHashService {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Hashes a raw password using BCrypt.
     *
     * @param rawPassword the plaintext password
     * @return the BCrypt hash
     */
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Verifies a raw password against a BCrypt hash.
     *
     * @param rawPassword the plaintext password to verify
     * @param hashedPassword the BCrypt hash to compare against
     * @return true if the password matches, false otherwise
     */
    public boolean matches(String rawPassword, String hashedPassword) {
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
}
