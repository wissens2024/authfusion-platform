package com.authfusion.agent.config;

import com.authfusion.agent.cc.ToeScope;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@ToeScope(value = "SSO Agent 설정 프로퍼티", sfr = {})
@Getter
@Setter
public class SsoAgentProperties {

    private String ssoServerUrl = "http://localhost:8081";
    private String clientId;
    private String clientSecret;
    private String callbackPath = "/sso/callback";
    private String logoutPath = "/sso/logout";
    private String scope = "openid profile email roles";
    private String sessionCookieName = "SSO_AGENT_SESSION";
    private int sessionTimeout = 3600;
    private boolean requireHttps = false;
    private long jwksCacheDuration = 3600;
    private List<String> excludedPaths = new ArrayList<>();
    private List<AccessRuleConfig> accessRules = new ArrayList<>();

    @Getter
    @Setter
    public static class AccessRuleConfig {
        private String pattern;
        private List<String> roles = new ArrayList<>();
        private boolean authenticated = true;
    }
}
