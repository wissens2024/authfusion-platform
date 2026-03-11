package com.authfusion.sso.oidc.endpoint;

import com.authfusion.sso.jwt.JwtTokenParser;
import com.authfusion.sso.jwt.TokenClaims;
import com.authfusion.sso.oidc.model.OidcError;
import com.authfusion.sso.oidc.model.UserInfoResponse;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.authfusion.sso.cc.ToeScope;

import java.util.Optional;
import java.util.UUID;

@ToeScope(value = "사용자 정보 엔드포인트", sfr = {"FIA_UAU.1"})
@RestController
@Tag(name = "OIDC UserInfo")
@RequiredArgsConstructor
@Slf4j
public class UserInfoEndpoint {

    private final JwtTokenParser jwtTokenParser;
    private final UserRepository userRepository;

    @GetMapping("/oauth2/userinfo")
    @Operation(summary = "UserInfo endpoint")
    public ResponseEntity<?> userInfo(
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(OidcError.builder()
                            .error("invalid_token")
                            .errorDescription("Bearer token is required")
                            .build());
        }

        String token = authorization.substring(7);
        Optional<TokenClaims> claimsOpt = jwtTokenParser.parseAndValidate(token);

        if (claimsOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(OidcError.builder()
                            .error("invalid_token")
                            .errorDescription("Invalid or expired token")
                            .build());
        }

        TokenClaims claims = claimsOpt.get();

        UserInfoResponse.UserInfoResponseBuilder builder = UserInfoResponse.builder()
                .sub(claims.getSub());

        try {
            UUID userId = UUID.fromString(claims.getSub());
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            userOpt.ifPresent(user -> {
                builder.preferredUsername(user.getUsername());
                builder.email(user.getEmail());
                builder.emailVerified(user.isEmailVerified());
                builder.givenName(user.getFirstName());
                builder.familyName(user.getLastName());
                String fullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                        (user.getLastName() != null ? " " + user.getLastName() : "");
                builder.name(fullName.trim());
            });
        } catch (IllegalArgumentException e) {
            // sub is not a UUID (e.g., client_credentials)
        }

        if (claims.getRoles() != null) {
            builder.roles(claims.getRoles());
        }

        return ResponseEntity.ok(builder.build());
    }
}
