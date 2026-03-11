package com.authfusion.agent.filter;

import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.config.SsoAgentProperties;
import com.authfusion.agent.context.SsoSecurityContextHolder;
import com.authfusion.agent.session.AgentSessionManager;
import com.authfusion.agent.util.CookieHelper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@ToeScope(value = "SSO 로그아웃 필터", sfr = {"FTA_SSL.3"})
public class SsoLogoutFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SsoLogoutFilter.class);

    private final SsoAgentProperties properties;
    private final AgentSessionManager sessionManager;

    public SsoLogoutFilter(SsoAgentProperties properties, AgentSessionManager sessionManager) {
        this.properties = properties;
        this.sessionManager = sessionManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().equals(properties.getLogoutPath())) {
            chain.doFilter(request, response);
            return;
        }

        // Clear local session
        String sessionId = CookieHelper.getCookieValue(httpRequest, properties.getSessionCookieName());
        if (sessionId != null) {
            sessionManager.invalidateSession(sessionId);
        }

        CookieHelper.deleteCookie(httpResponse, properties.getSessionCookieName(), "/");
        SsoSecurityContextHolder.clearContext();

        // Invalidate HTTP session
        if (httpRequest.getSession(false) != null) {
            httpRequest.getSession().invalidate();
        }

        // Redirect to SSO Server logout
        String postLogoutRedirectUri = getBaseUrl(httpRequest);
        String logoutUrl = properties.getSsoServerUrl() + "/oauth2/logout" +
                "?post_logout_redirect_uri=" + URLEncoder.encode(postLogoutRedirectUri, StandardCharsets.UTF_8);

        log.info("User logged out, redirecting to SSO server logout");
        httpResponse.sendRedirect(logoutUrl);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (("http".equals(scheme) && serverPort != 80) ||
                ("https".equals(scheme) && serverPort != 443)) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);
        return url.toString();
    }
}
