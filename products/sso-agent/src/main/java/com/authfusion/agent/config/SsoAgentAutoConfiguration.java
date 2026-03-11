package com.authfusion.agent.config;

import com.authfusion.agent.access.AccessControlManager;
import com.authfusion.agent.access.AccessRule;
import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.filter.SsoAuthenticationFilter;
import com.authfusion.agent.filter.SsoAuthorizationFilter;
import com.authfusion.agent.filter.SsoLogoutFilter;
import com.authfusion.agent.session.AgentSessionManager;
import com.authfusion.agent.token.JwksKeyResolver;
import com.authfusion.agent.token.JwtTokenValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ToeScope(value = "SSO Agent 자동 설정", sfr = {})
@Configuration
public class SsoAgentAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "authfusion.agent")
    @ConditionalOnMissingBean
    public SsoAgentProperties ssoAgentProperties() {
        return new SsoAgentProperties();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwksKeyResolver jwksKeyResolver(SsoAgentProperties properties) {
        return new JwksKeyResolver(properties.getSsoServerUrl(), properties.getJwksCacheDuration());
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenValidator jwtTokenValidator(JwksKeyResolver keyResolver) {
        return new JwtTokenValidator(keyResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public AgentSessionManager agentSessionManager(SsoAgentProperties properties) {
        return new AgentSessionManager(properties.getSessionTimeout());
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessControlManager accessControlManager(SsoAgentProperties properties) {
        AccessControlManager manager = new AccessControlManager();
        for (SsoAgentProperties.AccessRuleConfig ruleConfig : properties.getAccessRules()) {
            manager.addRule(AccessRule.builder()
                    .pattern(ruleConfig.getPattern())
                    .requiredRoles(ruleConfig.getRoles())
                    .authenticated(ruleConfig.isAuthenticated())
                    .build());
        }
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoAuthenticationFilter ssoAuthenticationFilter(SsoAgentProperties properties,
                                                            AgentSessionManager sessionManager,
                                                            JwtTokenValidator tokenValidator) {
        return new SsoAuthenticationFilter(properties, sessionManager, tokenValidator);
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoAuthorizationFilter ssoAuthorizationFilter(AccessControlManager accessControlManager) {
        return new SsoAuthorizationFilter(accessControlManager);
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoLogoutFilter ssoLogoutFilter(SsoAgentProperties properties,
                                            AgentSessionManager sessionManager) {
        return new SsoLogoutFilter(properties, sessionManager);
    }
}
