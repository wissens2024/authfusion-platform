package com.authfusion.sso.config;

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

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isPresent()) {
            log.info("Admin user already exists, skipping initialization");
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
        final UserEntity savedAdmin = userRepository.save(admin);

        roleRepository.findByName("ADMIN").ifPresent(role -> {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setUserId(savedAdmin.getId());
            userRole.setRoleId(role.getId());
            userRoleRepository.save(userRole);
        });

        log.info("=== Initial admin user created ===");
        log.info("Username: admin");
        log.info("Password: Admin1234!");
        log.info("=== Please change the password after first login ===");
    }
}
