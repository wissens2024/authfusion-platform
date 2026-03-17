package com.authfusion.sso.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.authfusion.sso.cc.ToeScope;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ToeScope(value = "JWT 토큰 생성/서명", sfr = {"FCS_COP.1", "FCS_CKM.1"})
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final KeyPairManager keyPairManager;

    @Value("${authfusion.sso.issuer}")
    private String issuer;

    @Value("${authfusion.sso.jwt.access-token-validity:300}")
    private long accessTokenValidity;

    @Value("${authfusion.sso.jwt.refresh-token-validity:86400}")
    private long refreshTokenValidity;

    @Value("${authfusion.sso.jwt.id-token-validity:3600}")
    private long idTokenValidity;

    public String generateAccessToken(TokenClaims claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(accessTokenValidity);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(claims.getSub())
                .issuer(issuer)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("scope", claims.getScope())
                .claim("client_id", claims.getClientId())
                .claim("token_type", "access_token");

        if (claims.getAud() != null && !claims.getAud().isEmpty()) {
            builder.audience(claims.getAud());
        }
        if (claims.getRoles() != null) {
            builder.claim("roles", claims.getRoles());
        }
        if (claims.getPreferredUsername() != null) {
            builder.claim("preferred_username", claims.getPreferredUsername());
        }
        if (claims.getEmail() != null) {
            builder.claim("email", claims.getEmail());
        }

        return signJwt(builder.build());
    }

    public String generateIdToken(TokenClaims claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(idTokenValidity);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(claims.getSub())
                .issuer(issuer)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("token_type", "id_token");

        if (claims.getAud() != null && !claims.getAud().isEmpty()) {
            builder.audience(claims.getAud());
        }
        if (claims.getNonce() != null) {
            builder.claim("nonce", claims.getNonce());
        }
        if (claims.getPreferredUsername() != null) {
            builder.claim("preferred_username", claims.getPreferredUsername());
        }
        if (claims.getEmail() != null) {
            builder.claim("email", claims.getEmail());
            builder.claim("email_verified", claims.isEmailVerified());
        }
        if (claims.getGivenName() != null) {
            builder.claim("given_name", claims.getGivenName());
        }
        if (claims.getFamilyName() != null) {
            builder.claim("family_name", claims.getFamilyName());
        }
        if (claims.getRoles() != null) {
            builder.claim("roles", claims.getRoles());
        }

        return signJwt(builder.build());
    }

    public String generateRefreshToken(TokenClaims claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(refreshTokenValidity);

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(claims.getSub())
                .issuer(issuer)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("scope", claims.getScope())
                .claim("client_id", claims.getClientId())
                .claim("token_type", "refresh_token")
                .build();

        return signJwt(claimsSet);
    }

    private final JwtTokenParser jwtTokenParser;

    public Optional<TokenClaims> parseRefreshToken(String token) {
        Optional<TokenClaims> claims = jwtTokenParser.parseAndValidate(token);
        if (claims.isEmpty()) return Optional.empty();
        // refresh_token인지 확인 (scope에서 token_type 구분은 JwtTokenParser가 안하므로 여기서)
        return claims;
    }

    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    private String signJwt(JWTClaimsSet claimsSet) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyPairManager.getActiveKid())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            JWSSigner signer = new RSASSASigner(keyPairManager.getPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Failed to sign JWT", e);
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }
}
