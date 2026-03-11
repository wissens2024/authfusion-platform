package com.authfusion.agent.access;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.context.SsoSecurityContext;

import java.util.List;

@ToeScope(value = "역할 기반 접근 검사", sfr = {"FDP_ACC.1", "FDP_ACF.1"})
public class RoleBasedAccessChecker {

    public boolean hasAccess(SsoSecurityContext context, List<String> requiredRoles) {
        if (requiredRoles == null || requiredRoles.isEmpty()) {
            return true;
        }
        if (context == null || !context.isAuthenticated()) {
            return false;
        }
        return requiredRoles.stream().anyMatch(context::hasRole);
    }
}
