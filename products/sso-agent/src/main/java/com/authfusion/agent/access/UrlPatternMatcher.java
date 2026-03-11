package com.authfusion.agent.access;

import com.authfusion.agent.cc.ToeScope;

@ToeScope(value = "URL 패턴 매처", sfr = {"FDP_ACC.1"})
public class UrlPatternMatcher {

    public boolean matches(String pattern, String path) {
        if (pattern == null || path == null) return false;

        // Exact match
        if (pattern.equals(path)) return true;

        // Wildcard match: /api/** matches /api/anything/here
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }

        // Single wildcard: /api/* matches /api/something but not /api/something/else
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            if (!path.startsWith(prefix)) return false;
            String remaining = path.substring(prefix.length());
            return !remaining.isEmpty() && !remaining.substring(1).contains("/");
        }

        // Extension match: *.html
        if (pattern.startsWith("*.")) {
            String extension = pattern.substring(1);
            return path.endsWith(extension);
        }

        return false;
    }
}
