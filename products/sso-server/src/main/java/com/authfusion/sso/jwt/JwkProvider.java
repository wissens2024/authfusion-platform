package com.authfusion.sso.jwt;

import com.authfusion.sso.cc.ToeScope;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ToeScope(value = "JWK 공개키 제공", sfr = {"FCS_CKM.1"})
@Component
@RequiredArgsConstructor
public class JwkProvider {

    private final KeyPairManager keyPairManager;

    public JWKSet getJwkSet() {
        List<JWK> keys = new ArrayList<>();
        Map<String, RSAPublicKey> allKeys = keyPairManager.getAllPublicKeys();
        for (Map.Entry<String, RSAPublicKey> entry : allKeys.entrySet()) {
            JWK jwk = new RSAKey.Builder(entry.getValue())
                    .keyID(entry.getKey())
                    .keyUse(KeyUse.SIGNATURE)
                    .build();
            keys.add(jwk);
        }
        return new JWKSet(keys);
    }
}
