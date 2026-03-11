package com.authfusion.sso.ldap.config;

import com.authfusion.sso.ldap.service.LdapConnectionService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "authfusion.sso.ldap.enabled", havingValue = "true")
public class LdapHealthIndicator implements HealthIndicator {

    private final LdapConnectionService connectionService;

    public LdapHealthIndicator(LdapConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public Health health() {
        try {
            boolean reachable = connectionService.testConnection();
            if (reachable) {
                return Health.up()
                        .withDetail("ldap", "Connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("ldap", "Unreachable")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("ldap", "Error: " + e.getMessage())
                    .build();
        }
    }
}
