package com.authfusion.sso.oidc.service;

import org.springframework.stereotype.Service;

import com.authfusion.sso.cc.ToeScope;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@ToeScope(value = "스코프 관리 서비스", sfr = {"FDP_ACC.1"})
@Service
public class ScopeService {

    private static final Set<String> SUPPORTED_SCOPES = Set.of(
            "openid", "profile", "email", "roles", "offline_access"
    );

    public Set<String> validateScopes(String scopeString) {
        if (scopeString == null || scopeString.isBlank()) {
            return Set.of("openid");
        }

        Set<String> requested = new LinkedHashSet<>(Arrays.asList(scopeString.split("\\s+")));
        requested.retainAll(SUPPORTED_SCOPES);

        if (requested.isEmpty()) {
            requested.add("openid");
        }

        return requested;
    }

    public boolean isValidScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return false;
        }
        return Arrays.stream(scope.split("\\s+"))
                .allMatch(SUPPORTED_SCOPES::contains);
    }

    public boolean containsScope(String scopeString, String target) {
        if (scopeString == null) {
            return false;
        }
        return Arrays.asList(scopeString.split("\\s+")).contains(target);
    }
}
