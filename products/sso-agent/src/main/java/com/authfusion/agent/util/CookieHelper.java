package com.authfusion.agent.util;

import com.authfusion.agent.cc.ToeScope;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ToeScope(value = "쿠키 헬퍼", sfr = {"FTP_TRP.1"})
public final class CookieHelper {

    private CookieHelper() {}

    public static String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static void setCookie(HttpServletResponse response, String name, String value,
                                  int maxAge, boolean httpOnly, boolean secure, String path) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath(path != null ? path : "/");
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setPath(path != null ? path : "/");
        response.addCookie(cookie);
    }
}
