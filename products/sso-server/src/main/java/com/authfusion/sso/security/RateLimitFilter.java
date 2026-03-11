package com.authfusion.sso.security;

import com.authfusion.sso.cc.ToeScope;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@ToeScope(value = "요청 속도 제한 필터", sfr = {"FIA_AFL.1"})
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${authfusion.sso.security.rate-limit.requests-per-second:10}")
    private int requestsPerSecond;

    @Value("${authfusion.sso.security.rate-limit.burst-size:20}")
    private int burstSize;

    private final Cache<String, AtomicInteger> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(1))
            .maximumSize(50000)
            .build();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        AtomicInteger count = requestCounts.get(clientIp, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();

        if (currentCount > burstSize) {
            log.warn("Rate limit exceeded for IP: {} ({} requests)", clientIp, currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limit_exceeded\",\"message\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
