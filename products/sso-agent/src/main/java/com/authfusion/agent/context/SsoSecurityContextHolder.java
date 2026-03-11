package com.authfusion.agent.context;

import com.authfusion.agent.cc.ToeScope;

@ToeScope(value = "SSO 보안 컨텍스트 홀더", sfr = {"FIA_USB.1"})
public final class SsoSecurityContextHolder {

    private static final ThreadLocal<SsoSecurityContext> contextHolder = new ThreadLocal<>();

    private SsoSecurityContextHolder() {}

    public static SsoSecurityContext getContext() {
        SsoSecurityContext context = contextHolder.get();
        if (context == null) {
            context = new SsoSecurityContext();
            contextHolder.set(context);
        }
        return context;
    }

    public static void setContext(SsoSecurityContext context) {
        contextHolder.set(context);
    }

    public static void clearContext() {
        contextHolder.remove();
    }

    public static boolean isAuthenticated() {
        SsoSecurityContext context = contextHolder.get();
        return context != null && context.isAuthenticated();
    }
}
