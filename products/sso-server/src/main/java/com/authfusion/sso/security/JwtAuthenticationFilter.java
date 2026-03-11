package com.authfusion.sso.security;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.jwt.JwtTokenParser;
import com.authfusion.sso.jwt.TokenClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@ToeScope(value = "JWT Bearer 토큰 인증 필터", sfr = {"FIA_UAU.1", "FIA_UID.1"})
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenParser jwtTokenParser;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Optional<TokenClaims> claims = jwtTokenParser.parseAndValidate(token);

            if (claims.isPresent()) {
                TokenClaims tokenClaims = claims.get();
                List<SimpleGrantedAuthority> authorities = tokenClaims.getRoles() != null
                        ? tokenClaims.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .toList()
                        : Collections.emptyList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                tokenClaims.getSub(),
                                null,
                                authorities);
                authentication.setDetails(tokenClaims);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT authentication successful for user: {}", tokenClaims.getSub());
            } else {
                log.debug("JWT token validation failed");
            }
        }

        filterChain.doFilter(request, response);
    }
}
