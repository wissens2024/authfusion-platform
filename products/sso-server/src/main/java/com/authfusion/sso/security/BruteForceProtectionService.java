package com.authfusion.sso.security;

import com.authfusion.sso.cc.ToeScope;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@ToeScope(value = "무차별 대입 보호", sfr = {"FIA_AFL.1"})
@Service
@Slf4j
public class BruteForceProtectionService {

    @Value("${authfusion.sso.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${authfusion.sso.security.lockout-duration:1800}")
    private long lockoutDuration;

    private final Cache<String, AtomicInteger> attemptsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(10000)
            .build();

    private final Cache<String, Boolean> lockedCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(10000)
            .build();

    public void recordFailedAttempt(String key) {
        AtomicInteger attempts = attemptsCache.get(key, k -> new AtomicInteger(0));
        int count = attempts.incrementAndGet();
        log.debug("Failed login attempt {} of {} for key: {}", count, maxAttempts, key);

        if (count >= maxAttempts) {
            lockedCache.put(key, true);
            log.warn("Account locked due to {} failed attempts for key: {}", count, key);
        }
    }

    public void recordSuccessfulAttempt(String key) {
        attemptsCache.invalidate(key);
        lockedCache.invalidate(key);
    }

    public boolean isBlocked(String key) {
        return Boolean.TRUE.equals(lockedCache.getIfPresent(key));
    }

    public int getFailedAttempts(String key) {
        AtomicInteger attempts = attemptsCache.getIfPresent(key);
        return attempts != null ? attempts.get() : 0;
    }
}
