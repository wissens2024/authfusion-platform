package com.authfusion.sso.oidc.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.authfusion.sso.cc.ToeScope;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@ToeScope(value = "PKCE 검증", sfr = {"FIA_UAU.1"})
@Component
@Slf4j
public class PkceValidator {

    public boolean validate(String codeVerifier, String codeChallenge, String codeChallengeMethod) {
        if (codeChallenge == null || codeVerifier == null) {
            return false;
        }

        if ("plain".equals(codeChallengeMethod)) {
            return codeChallenge.equals(codeVerifier);
        }

        // Default to S256
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return codeChallenge.equals(computed);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available", e);
            return false;
        }
    }

    public boolean isValidCodeVerifier(String codeVerifier) {
        if (codeVerifier == null) {
            return false;
        }
        return codeVerifier.length() >= 43 && codeVerifier.length() <= 128;
    }
}
