package com.authfusion.agent.filter;

import com.authfusion.agent.access.AccessControlManager;
import com.authfusion.agent.cc.ToeScope;
import com.authfusion.agent.context.SsoSecurityContext;
import com.authfusion.agent.context.SsoSecurityContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@ToeScope(value = "SSO 인가 필터", sfr = {"FDP_ACC.1"})
public class SsoAuthorizationFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SsoAuthorizationFilter.class);

    private final AccessControlManager accessControlManager;

    public SsoAuthorizationFilter(AccessControlManager accessControlManager) {
        this.accessControlManager = accessControlManager;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestPath = httpRequest.getRequestURI();

        SsoSecurityContext context = SsoSecurityContextHolder.getContext();
        AccessControlManager.AccessDecision decision = accessControlManager.checkAccess(requestPath, context);

        switch (decision) {
            case ALLOWED -> chain.doFilter(request, response);
            case AUTHENTICATION_REQUIRED -> {
                log.debug("Authentication required for path: {}", requestPath);
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            }
            case FORBIDDEN -> {
                log.warn("Access denied for user {} to path: {}",
                        context != null ? context.getUsername() : "anonymous", requestPath);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            }
        }
    }
}
