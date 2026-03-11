package com.authfusion.agent.token;

import com.authfusion.agent.cc.ToeScope;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@ToeScope(value = "JWT 토큰 검증기", sfr = {"FCS_COP.1"})
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwksKeyResolver keyResolver;
    private final TokenCache tokenCache;

    public JwtTokenValidator(JwksKeyResolver keyResolver) {
        this.keyResolver = keyResolver;
        this.tokenCache = new TokenCache(10000);
    }

    public Optional<TokenInfo> validate(String token) {
        // Check cache first
        TokenInfo cached = tokenCache.get(token);
        if (cached != null) {
            if (!cached.isExpired()) {
                return Optional.of(cached);
            }
            tokenCache.invalidate(token);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String kid = signedJWT.getHeader().getKeyID();

            RSAPublicKey publicKey = keyResolver.resolveKey(kid);
            if (publicKey == null) {
                log.warn("No public key found for kid: {}", kid);
                return Optional.empty();
            }

            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                log.warn("JWT signature verification failed");
                return Optional.empty();
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            if (claims.getExpirationTime() != null &&
                    claims.getExpirationTime().before(Date.from(Instant.now()))) {
                log.debug("JWT token expired");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = claims.getStringListClaim("roles");

            TokenInfo tokenInfo = TokenInfo.builder()
                    .subject(claims.getSubject())
                    .issuer(claims.getIssuer())
                    .audience(claims.getAudience())
                    .issuedAt(claims.getIssueTime() != null ? claims.getIssueTime().toInstant() : null)
                    .expiresAt(claims.getExpirationTime() != null ? claims.getExpirationTime().toInstant() : null)
                    .jwtId(claims.getJWTID())
                    .scope(claims.getStringClaim("scope"))
                    .clientId(claims.getStringClaim("client_id"))
                    .preferredUsername(claims.getStringClaim("preferred_username"))
                    .email(claims.getStringClaim("email"))
                    .roles(roles)
                    .build();

            tokenCache.put(token, tokenInfo);
            return Optional.of(tokenInfo);

        } catch (ParseException | com.nimbusds.jose.JOSEException e) {
            log.error("Failed to validate JWT token", e);
            return Optional.empty();
        }
    }
}
