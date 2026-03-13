package com.authfusion.sso.config;

import com.authfusion.sso.client.model.ClientEntity;
import com.authfusion.sso.client.model.ClientType;
import com.authfusion.sso.client.repository.ClientRepository;
import com.authfusion.sso.rbac.model.RoleEntity;
import com.authfusion.sso.rbac.model.UserRoleEntity;
import com.authfusion.sso.rbac.repository.RoleRepository;
import com.authfusion.sso.rbac.repository.UserRoleRepository;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.model.UserStatus;
import com.authfusion.sso.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public void run(String... args) {
        initAdminUser();
        initTestUser();
        initTestClients();
    }

    private void initAdminUser() {
        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("Admin user already exists, skipping");
            return;
        }
        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setEmail("admin@authfusion.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin1234!"));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setStatus(UserStatus.ACTIVE);
        admin.setEmailVerified(true);
        final UserEntity saved = userRepository.save(admin);
        assignRole(saved, "ADMIN");
        log.info("✓ Admin user created: admin / Admin1234!");
    }

    private void initTestUser() {
        if (userRepository.findByUsername("testuser").isPresent()) return;
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("testuser@authfusion.local");
        user.setPasswordHash(passwordEncoder.encode("Test1234!"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        final UserEntity saved = userRepository.save(user);
        assignRole(saved, "USER");
        log.info("✓ Test user created: testuser / Test1234!");
    }

    private void assignRole(UserEntity user, String roleName) {
        roleRepository.findByName(roleName).ifPresent(role -> {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(user.getId());
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        });
    }

    private void initTestClients() {
        // Admin Console client
        if (!clientRepository.existsByClientId("admin-console")) {
            clientRepository.save(ClientEntity.builder()
                    .clientId("admin-console")
                    .clientSecretHash(passwordEncoder.encode("admin-console-secret"))
                    .clientName("AuthFusion Admin Console")
                    .clientType(ClientType.CONFIDENTIAL)
                    .redirectUris("[\"http://localhost:3001/callback\",\"https://sso.aines.kr/callback\"]")
                    .allowedScopes("[\"openid\",\"profile\",\"email\",\"roles\"]")
                    .allowedGrantTypes("[\"authorization_code\",\"refresh_token\"]")
                    .requirePkce(true)
                    .accessTokenValidity(3600)
                    .refreshTokenValidity(86400)
                    .build());
            log.info("✓ Client created: admin-console");
        }

        // Demo Web App (Public PKCE client)
        if (!clientRepository.existsByClientId("demo-web-app")) {
            clientRepository.save(ClientEntity.builder()
                    .clientId("demo-web-app")
                    .clientName("Demo Web Application")
                    .clientType(ClientType.PUBLIC)
                    .redirectUris("[\"http://localhost:8081/callback\",\"http://localhost:3001/callback\"]")
                    .allowedScopes("[\"openid\",\"profile\",\"email\"]")
                    .allowedGrantTypes("[\"authorization_code\",\"refresh_token\"]")
                    .requirePkce(true)
                    .accessTokenValidity(1800)
                    .refreshTokenValidity(43200)
                    .build());
            log.info("✓ Client created: demo-web-app (PUBLIC/PKCE)");
        }

        // Machine-to-Machine client
        if (!clientRepository.existsByClientId("m2m-service")) {
            clientRepository.save(ClientEntity.builder()
                    .clientId("m2m-service")
                    .clientSecretHash(passwordEncoder.encode("m2m-service-secret-2026"))
                    .clientName("M2M Service (Backend)")
                    .clientType(ClientType.CONFIDENTIAL)
                    .redirectUris("[]")
                    .allowedScopes("[\"openid\",\"profile\"]")
                    .allowedGrantTypes("[\"client_credentials\"]")
                    .requirePkce(false)
                    .accessTokenValidity(900)
                    .refreshTokenValidity(0)
                    .build());
            log.info("✓ Client created: m2m-service (client_credentials)");
        }
    }
}
