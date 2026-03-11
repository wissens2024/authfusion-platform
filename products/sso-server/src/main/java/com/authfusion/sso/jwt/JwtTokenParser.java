package com.authfusion.sso.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.authfusion.sso.cc.ToeScope;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@ToeScope(value = "JWT 토큰 검증/파싱", sfr = {"FCS_COP.1"})
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenParser {

    private final KeyPairManager keyPairManager;

    @Value("${authfusion.sso.issuer}")
    private String issuer;

    public Optional<TokenClaims> parseAndValidate(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new RSASSAVerifier(keyPairManager.getPublicKey());
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

            if (!issuer.equals(claims.getIssuer())) {
                log.warn("JWT issuer mismatch: expected={}, actual={}", issuer, claims.getIssuer());
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<String> roles = claims.getStringListClaim("roles");

            return Optional.of(TokenClaims.builder()
                    .sub(claims.getSubject())
                    .iss(claims.getIssuer())
                    .aud(claims.getAudience())
                    .iat(claims.getIssueTime() != null ? claims.getIssueTime().toInstant() : null)
                    .exp(claims.getExpirationTime() != null ? claims.getExpirationTime().toInstant() : null)
                    .jti(claims.getJWTID())
                    .scope(claims.getStringClaim("scope"))
                    .clientId(claims.getStringClaim("client_id"))
                    .preferredUsername(claims.getStringClaim("preferred_username"))
                    .email(claims.getStringClaim("email"))
                    .roles(roles)
                    .build());
        } catch (ParseException | JOSEException e) {
            log.error("Failed to parse JWT token", e);
            return Optional.empty();
        }
    }

    public Optional<JWTClaimsSet> parseClaims(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return Optional.of(signedJWT.getJWTClaimsSet());
        } catch (ParseException e) {
            log.error("Failed to parse JWT claims", e);
            return Optional.empty();
        }
    }
}
