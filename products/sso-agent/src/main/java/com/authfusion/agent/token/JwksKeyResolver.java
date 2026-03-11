package com.authfusion.agent.token;

import com.authfusion.agent.cc.ToeScope;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@ToeScope(value = "JWKS 키 리졸버", sfr = {"FCS_CKM.1"})
public class JwksKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(JwksKeyResolver.class);

    private final String jwksUrl;
    private final long cacheDurationSeconds;
    private final HttpClient httpClient;
    private final ReentrantLock lock = new ReentrantLock();

    private JWKSet cachedJwkSet;
    private Instant cacheExpiry;

    public JwksKeyResolver(String ssoServerUrl, long cacheDurationSeconds) {
        this.jwksUrl = ssoServerUrl + "/.well-known/jwks.json";
        this.cacheDurationSeconds = cacheDurationSeconds;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public RSAPublicKey resolveKey(String kid) {
        JWKSet jwkSet = getJwkSet();
        if (jwkSet == null) return null;

        JWK jwk = jwkSet.getKeyByKeyId(kid);
        if (jwk == null) {
            // Force refresh in case key was rotated
            invalidateCache();
            jwkSet = getJwkSet();
            if (jwkSet == null) return null;
            jwk = jwkSet.getKeyByKeyId(kid);
        }

        if (jwk instanceof RSAKey rsaKey) {
            try {
                return rsaKey.toRSAPublicKey();
            } catch (Exception e) {
                log.error("Failed to convert JWK to RSA public key", e);
            }
        }

        return null;
    }

    private JWKSet getJwkSet() {
        if (cachedJwkSet != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry)) {
            return cachedJwkSet;
        }

        lock.lock();
        try {
            // Double-check after acquiring lock
            if (cachedJwkSet != null && cacheExpiry != null && Instant.now().isBefore(cacheExpiry)) {
                return cachedJwkSet;
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                cachedJwkSet = JWKSet.parse(response.body());
                cacheExpiry = Instant.now().plusSeconds(cacheDurationSeconds);
                log.debug("JWKS refreshed from {}", jwksUrl);
                return cachedJwkSet;
            } else {
                log.error("Failed to fetch JWKS: HTTP {}", response.statusCode());
            }
        } catch (IOException | InterruptedException | ParseException e) {
            log.error("Failed to fetch JWKS from {}", jwksUrl, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        } finally {
            lock.unlock();
        }

        return cachedJwkSet; // Return stale cache if available
    }

    public void invalidateCache() {
        cacheExpiry = null;
    }
}
