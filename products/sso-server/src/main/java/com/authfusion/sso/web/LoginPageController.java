package com.authfusion.sso.web;

import com.authfusion.sso.audit.service.AuditService;
import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.mfa.model.MfaPendingSessionEntity;
import com.authfusion.sso.mfa.service.MfaSessionService;
import com.authfusion.sso.mfa.service.TotpService;
import com.authfusion.sso.oidc.service.AuthorizationService;
import com.authfusion.sso.security.BruteForceProtectionService;
import com.authfusion.sso.session.model.SsoSession;
import com.authfusion.sso.session.service.SessionService;
import com.authfusion.sso.user.model.UserEntity;
import com.authfusion.sso.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@ToeScope(value = "로그인 페이지 컨트롤러", sfr = {"FIA_UAU.1"})
@Controller
@RequiredArgsConstructor
@Slf4j
public class LoginPageController {

    private final UserService userService;
    private final SessionService sessionService;
    private final AuthorizationService authorizationService;
    private final BruteForceProtectionService bruteForceService;
    private final AuditService auditService;
    private final JdbcTemplate jdbcTemplate;
    private final TotpService totpService;
    private final MfaSessionService mfaSessionService;

    @Value("${authfusion.sso.jwt.authorization-code-validity:600}")
    private long codeValidity;

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "response_type", required = false) String responseType,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            @RequestParam(value = "error", required = false) String error,
            Model model) {
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("scope", scope);
        model.addAttribute("responseType", responseType);
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);
        model.addAttribute("codeChallenge", codeChallenge);
        model.addAttribute("codeChallengeMethod", codeChallengeMethod);
        model.addAttribute("error", error);
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "response_type", required = false) String responseType,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "code_challenge", required = false) String codeChallenge,
            @RequestParam(value = "code_challenge_method", required = false) String codeChallengeMethod,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model) {

        String ip = getClientIp(httpRequest);

        if (bruteForceService.isBlocked(username)) {
            model.addAttribute("error", "Account is temporarily locked due to too many failed attempts");
            addOAuthParams(model, clientId, redirectUri, scope, responseType, state, nonce, codeChallenge, codeChallengeMethod);
            return "login";
        }

        try {
            UserEntity user = userService.authenticate(username, password);
            bruteForceService.recordSuccessfulAttempt(username);
            auditService.logAuthentication("LOGIN", user.getId().toString(), ip, true, null);

            // Check if TOTP MFA is enabled
            if (totpService.isTotpEnabled(user.getId())) {
                MfaPendingSessionEntity pending = mfaSessionService.createPendingSession(
                        user.getId(), ip, httpRequest.getHeader("User-Agent"),
                        clientId, redirectUri, scope, responseType, state, nonce,
                        codeChallenge, codeChallengeMethod);
                return "redirect:/login/mfa?mfa_token=" + encode(pending.getMfaToken());
            }

            return completeLogin(user, ip, httpRequest, httpResponse,
                    clientId, redirectUri, scope, codeChallenge, codeChallengeMethod, nonce, state);
        } catch (Exception e) {
            bruteForceService.recordFailedAttempt(username);
            auditService.logAuthentication("LOGIN", username, ip, false, e.getMessage());
            model.addAttribute("error", "Invalid username or password");
            addOAuthParams(model, clientId, redirectUri, scope, responseType, state, nonce, codeChallenge, codeChallengeMethod);
            return "login";
        }
    }

    @GetMapping("/login/mfa")
    public String mfaPage(@RequestParam("mfa_token") String mfaToken, Model model) {
        try {
            mfaSessionService.validateAndGet(mfaToken);
        } catch (Exception e) {
            model.addAttribute("error", "MFA session expired. Please login again.");
            return "login";
        }
        model.addAttribute("mfaToken", mfaToken);
        return "mfa-verify";
    }

    @PostMapping("/login/mfa")
    public String verifyMfa(
            @RequestParam("mfa_token") String mfaToken,
            @RequestParam("code") String code,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model) {

        String ip = getClientIp(httpRequest);

        try {
            MfaPendingSessionEntity pending = mfaSessionService.validateAndGet(mfaToken);

            boolean valid = totpService.verifyTotp(pending.getUserId(), code);
            if (!valid) {
                model.addAttribute("error", "Invalid verification code");
                model.addAttribute("mfaToken", mfaToken);
                auditService.logAuthentication("MFA_VERIFY", pending.getUserId().toString(), ip, false, "Invalid TOTP code");
                return "mfa-verify";
            }

            auditService.logAuthentication("MFA_VERIFY", pending.getUserId().toString(), ip, true, null);
            UserEntity user = userService.getUserEntity(pending.getUserId());

            String result = completeLogin(user, ip, httpRequest, httpResponse,
                    pending.getClientId(), pending.getRedirectUri(), pending.getScope(),
                    pending.getCodeChallenge(), pending.getCodeChallengeMethod(),
                    pending.getNonce(), pending.getState());

            mfaSessionService.consumePendingSession(mfaToken);
            return result;
        } catch (Exception e) {
            model.addAttribute("error", "MFA verification failed. Please try again.");
            model.addAttribute("mfaToken", mfaToken);
            return "mfa-verify";
        }
    }

    private String completeLogin(UserEntity user, String ip, HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse, String clientId, String redirectUri,
                                  String scope, String codeChallenge, String codeChallengeMethod,
                                  String nonce, String state) {
        SsoSession session = sessionService.createSession(
                user.getId(), user.getUsername(), ip,
                httpRequest.getHeader("User-Agent"));

        Cookie sessionCookie = new Cookie("SSO_SESSION", session.getSessionId());
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge((int) (session.getExpiresAt().getEpochSecond() -
                session.getCreatedAt().getEpochSecond()));
        httpResponse.addCookie(sessionCookie);

        if (clientId != null && redirectUri != null) {
            String code = authorizationService.generateAuthorizationCode();
            jdbcTemplate.update(
                    "INSERT INTO sso_authorization_codes (code, client_id, user_id, redirect_uri, scope, " +
                            "code_challenge, code_challenge_method, nonce, state, expires_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    code, clientId, user.getId(), redirectUri,
                    scope != null ? scope : "openid",
                    codeChallenge, codeChallengeMethod, nonce, state,
                    LocalDateTime.now().plusSeconds(codeValidity)
            );

            String url = redirectUri + "?code=" + encode(code) +
                    (state != null ? "&state=" + encode(state) : "");
            return "redirect:" + url;
        }

        return "redirect:/";
    }

    private void addOAuthParams(Model model, String clientId, String redirectUri, String scope,
                                 String responseType, String state, String nonce,
                                 String codeChallenge, String codeChallengeMethod) {
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUri", redirectUri);
        model.addAttribute("scope", scope);
        model.addAttribute("responseType", responseType);
        model.addAttribute("state", state);
        model.addAttribute("nonce", nonce);
        model.addAttribute("codeChallenge", codeChallenge);
        model.addAttribute("codeChallengeMethod", codeChallengeMethod);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
