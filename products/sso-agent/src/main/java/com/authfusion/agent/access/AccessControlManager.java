package com.authfusion.agent.access;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.context.SsoSecurityContext;

import java.util.ArrayList;
import java.util.List;

@ToeScope(value = "접근 제어 관리자", sfr = {"FDP_ACC.1"})
public class AccessControlManager {

    private final List<AccessRule> rules = new ArrayList<>();
    private final UrlPatternMatcher patternMatcher = new UrlPatternMatcher();
    private final RoleBasedAccessChecker roleChecker = new RoleBasedAccessChecker();

    public void addRule(AccessRule rule) {
        rules.add(rule);
    }

    public AccessDecision checkAccess(String requestPath, SsoSecurityContext context) {
        for (AccessRule rule : rules) {
            if (patternMatcher.matches(rule.getPattern(), requestPath)) {
                if (rule.isAuthenticated() && (context == null || !context.isAuthenticated())) {
                    return AccessDecision.AUTHENTICATION_REQUIRED;
                }
                if (rule.getRequiredRoles() != null && !rule.getRequiredRoles().isEmpty()) {
                    if (!roleChecker.hasAccess(context, rule.getRequiredRoles())) {
                        return AccessDecision.FORBIDDEN;
                    }
                }
                return AccessDecision.ALLOWED;
            }
        }
        return AccessDecision.ALLOWED; // No matching rule = allow by default
    }

    public enum AccessDecision {
        ALLOWED,
        AUTHENTICATION_REQUIRED,
        FORBIDDEN
    }
}
