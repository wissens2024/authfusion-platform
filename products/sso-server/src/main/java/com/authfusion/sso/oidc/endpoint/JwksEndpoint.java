package com.authfusion.sso.oidc.endpoint;

import com.authfusion.sso.jwt.JwkProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.authfusion.sso.cc.ToeScope;

import java.util.Map;

@ToeScope(value = "JWKS 공개키 엔드포인트", sfr = {"FCS_CKM.1"})
@RestController
@Tag(name = "OIDC Discovery")
@RequiredArgsConstructor
public class JwksEndpoint {

    private final JwkProvider jwkProvider;

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JSON Web Key Set")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(jwkProvider.getJwkSet().toJSONObject());
    }
}
