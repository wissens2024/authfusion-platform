package com.authfusion.sso.ldap.config;

import com.authfusion.sso.ldap.service.LdapAuthenticationService;
import com.authfusion.sso.ldap.service.LdapConnectionService;
import com.authfusion.sso.ldap.service.LdapUserSyncService;
import com.authfusion.sso.user.repository.UserRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "authfusion.sso.ldap.enabled", havingValue = "true")
@EnableConfigurationProperties(LdapProperties.class)
public class LdapAutoConfiguration {

    @Bean
    public LdapConnectionService ldapConnectionService(LdapProperties properties) {
        return new LdapConnectionService(properties);
    }

    @Bean
    public LdapAuthenticationService ldapAuthenticationService(LdapConnectionService connectionService,
                                                                LdapProperties properties) {
        return new LdapAuthenticationService(connectionService, properties);
    }

    @Bean
    public LdapUserSyncService ldapUserSyncService(LdapAuthenticationService authService,
                                                    UserRepository userRepository) {
        return new LdapUserSyncService(authService, userRepository);
    }
}
