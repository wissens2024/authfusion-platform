package com.authfusion.agent.token;

import com.authfusion.agent.cc.ToeScope;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ToeScope(value = "토큰 캐시", sfr = {"FCS_COP.1"})
public class TokenCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long maxSize;

    public TokenCache(long maxSize) {
        this.maxSize = maxSize;
    }

    public TokenInfo get(String token) {
        CacheEntry entry = cache.get(token);
        if (entry == null) return null;
        if (entry.expiresAt.isBefore(Instant.now())) {
            cache.remove(token);
            return null;
        }
        return entry.tokenInfo;
    }

    public void put(String token, TokenInfo tokenInfo) {
        if (cache.size() >= maxSize) {
            evictExpired();
        }
        Instant expiry = tokenInfo.getExpiresAt() != null
                ? tokenInfo.getExpiresAt()
                : Instant.now().plusSeconds(300);
        cache.put(token, new CacheEntry(tokenInfo, expiry));
    }

    public void invalidate(String token) {
        cache.remove(token);
    }

    public void clear() {
        cache.clear();
    }

    private void evictExpired() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> e.getValue().expiresAt.isBefore(now));
    }

    private record CacheEntry(TokenInfo tokenInfo, Instant expiresAt) {}
}
