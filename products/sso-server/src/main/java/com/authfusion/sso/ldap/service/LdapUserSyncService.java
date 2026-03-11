package com.authfusion.sso.ldap.service;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.ldap.model.LdapUserAttributes;
import com.authfusion.sso.ldap.model.UserSource;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.model.UserStatus;
import com.authfusion.sso.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ToeScope(value = "LDAP 사용자 동기화 서비스", sfr = {"FIA_UAU.1"})
@Slf4j
public class LdapUserSyncService {

    private final LdapAuthenticationService ldapAuthService;
    private final UserRepository userRepository;

    public LdapUserSyncService(LdapAuthenticationService ldapAuthService, UserRepository userRepository) {
        this.ldapAuthService = ldapAuthService;
        this.userRepository = userRepository;
    }

    /**
     * LDAP 인증 후 로컬 DB에 사용자 정보 동기화 (생성 또는 업데이트)
     */
    @Transactional
    public UserEntity syncUser(LdapUserAttributes ldapAttrs) {
        Optional<UserEntity> existingOpt = userRepository.findByUsername(ldapAttrs.getUsername());

        if (existingOpt.isPresent()) {
            UserEntity existing = existingOpt.get();
            // Update LDAP attributes
            if (ldapAttrs.getEmail() != null) {
                existing.setEmail(ldapAttrs.getEmail());
            }
            if (ldapAttrs.getFirstName() != null) {
                existing.setFirstName(ldapAttrs.getFirstName());
            }
            if (ldapAttrs.getLastName() != null) {
                existing.setLastName(ldapAttrs.getLastName());
            }
            existing.setUserSource(UserSource.LDAP.name());
            existing.setExternalId(ldapAttrs.getExternalId());
            existing.setLdapSyncedAt(LocalDateTime.now());
            UserEntity saved = userRepository.save(existing);
            log.debug("Synced existing user from LDAP: {}", saved.getUsername());
            return saved;
        }

        // Create new local user from LDAP
        UserEntity newUser = UserEntity.builder()
                .username(ldapAttrs.getUsername())
                .email(ldapAttrs.getEmail() != null ? ldapAttrs.getEmail() : ldapAttrs.getUsername() + "@ldap.local")
                .passwordHash("LDAP_MANAGED")
                .firstName(ldapAttrs.getFirstName())
                .lastName(ldapAttrs.getLastName())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .userSource(UserSource.LDAP.name())
                .externalId(ldapAttrs.getExternalId())
                .ldapSyncedAt(LocalDateTime.now())
                .build();

        UserEntity saved = userRepository.save(newUser);
        log.info("Created new user from LDAP: id={}, username='{}'", saved.getId(), saved.getUsername());
        return saved;
    }
}
