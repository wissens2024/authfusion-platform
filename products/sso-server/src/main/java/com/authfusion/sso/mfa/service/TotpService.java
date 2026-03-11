package com.authfusion.sso.mfa.service;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.config.GlobalExceptionHandler.AuthenticationException;
import com.authfusion.sso.config.GlobalExceptionHandler.ResourceNotFoundException;
import com.authfusion.sso.jwt.KeyEncryptionService;
import com.authfusion.sso.mfa.model.*;
import com.authfusion.sso.mfa.repository.RecoveryCodeRepository;
import com.authfusion.sso.mfa.repository.TotpSecretRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@ToeScope(value = "TOTP 서비스", sfr = {"FIA_UAU.1", "FCS_COP.1"})
@Service
@Slf4j
public class TotpService {

    private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final TotpSecretRepository totpSecretRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final KeyEncryptionService keyEncryptionService;
    private final PasswordEncoder passwordEncoder;

    @Value("${authfusion.sso.mfa.totp.issuer:AuthFusion SSO}")
    private String issuer;

    @Value("${authfusion.sso.mfa.totp.digits:6}")
    private int digits;

    @Value("${authfusion.sso.mfa.totp.period:30}")
    private int period;

    @Value("${authfusion.sso.mfa.totp.algorithm:HmacSHA1}")
    private String algorithm;

    @Value("${authfusion.sso.mfa.totp.time-step-window:1}")
    private int timeStepWindow;

    @Value("${authfusion.sso.mfa.totp.secret-length:20}")
    private int secretLength;

    @Value("${authfusion.sso.mfa.recovery-codes.count:10}")
    private int recoveryCodeCount;

    @Value("${authfusion.sso.mfa.recovery-codes.length:8}")
    private int recoveryCodeLength;

    public TotpService(TotpSecretRepository totpSecretRepository,
                       RecoveryCodeRepository recoveryCodeRepository,
                       KeyEncryptionService keyEncryptionService,
                       PasswordEncoder passwordEncoder) {
        this.totpSecretRepository = totpSecretRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.keyEncryptionService = keyEncryptionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TotpSetupResponse setupTotp(UUID userId, String username) {
        log.info("Setting up TOTP for user '{}'", username);

        // Delete existing unverified secret
        totpSecretRepository.findByUserId(userId).ifPresent(existing -> {
            if (existing.isEnabled()) {
                throw new IllegalStateException("TOTP is already enabled. Disable it first.");
            }
            totpSecretRepository.delete(existing);
        });

        // Generate secret
        byte[] secretBytes = new byte[secretLength];
        SECURE_RANDOM.nextBytes(secretBytes);
        String base32Secret = encodeBase32(secretBytes);

        // Encrypt secret for DB storage
        String[] encrypted = keyEncryptionService.encrypt(base32Secret);

        TotpSecretEntity entity = TotpSecretEntity.builder()
                .userId(userId)
                .encryptedSecret(encrypted[0])
                .iv(encrypted[1])
                .algorithm(algorithm)
                .digits(digits)
                .period(period)
                .verified(false)
                .enabled(false)
                .build();
        totpSecretRepository.save(entity);

        // Generate recovery codes
        List<String> recoveryCodes = generateAndSaveRecoveryCodes(userId);

        // Build otpauth URI
        String otpauthUri = buildOtpauthUri(username, base32Secret);

        // Generate QR code
        String qrCodeDataUri = generateQrCodeDataUri(otpauthUri);

        return TotpSetupResponse.builder()
                .secret(base32Secret)
                .qrCodeDataUri(qrCodeDataUri)
                .otpauthUri(otpauthUri)
                .recoveryCodes(recoveryCodes)
                .build();
    }

    @Transactional
    public void verifySetup(UUID userId, String code) {
        TotpSecretEntity entity = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TOTP not set up for this user"));

        if (entity.isEnabled()) {
            throw new IllegalStateException("TOTP is already enabled");
        }

        String secret = keyEncryptionService.decrypt(entity.getEncryptedSecret(), entity.getIv());

        if (!verifyCode(secret, code)) {
            throw new AuthenticationException("Invalid TOTP code");
        }

        entity.setVerified(true);
        entity.setEnabled(true);
        entity.setUpdatedAt(LocalDateTime.now());
        totpSecretRepository.save(entity);
        log.info("TOTP verified and enabled for userId={}", userId);
    }

    @Transactional
    public void disableTotp(UUID userId) {
        TotpSecretEntity entity = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TOTP not configured for this user"));

        totpSecretRepository.delete(entity);
        recoveryCodeRepository.deleteByUserId(userId);
        log.info("TOTP disabled for userId={}", userId);
    }

    @Transactional(readOnly = true)
    public MfaStatusResponse getMfaStatus(UUID userId) {
        Optional<TotpSecretEntity> totpOpt = totpSecretRepository.findByUserId(userId);
        int remainingCodes = recoveryCodeRepository.countByUserIdAndUsedFalse(userId);

        return MfaStatusResponse.builder()
                .userId(userId)
                .totpEnabled(totpOpt.map(TotpSecretEntity::isEnabled).orElse(false))
                .totpVerified(totpOpt.map(TotpSecretEntity::isVerified).orElse(false))
                .recoveryCodesRemaining(remainingCodes)
                .totpEnabledAt(totpOpt.map(TotpSecretEntity::getCreatedAt).orElse(null))
                .build();
    }

    public boolean isTotpEnabled(UUID userId) {
        return totpSecretRepository.existsByUserIdAndEnabledTrue(userId);
    }

    @Transactional
    public boolean verifyTotp(UUID userId, String code) {
        TotpSecretEntity entity = totpSecretRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TOTP not configured"));

        if (!entity.isEnabled()) {
            throw new IllegalStateException("TOTP is not enabled");
        }

        String secret = keyEncryptionService.decrypt(entity.getEncryptedSecret(), entity.getIv());

        // Try TOTP verification first
        if (verifyCode(secret, code)) {
            return true;
        }

        // Try recovery code
        return verifyRecoveryCode(userId, code);
    }

    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId) {
        if (!isTotpEnabled(userId)) {
            throw new IllegalStateException("TOTP must be enabled to regenerate recovery codes");
        }
        recoveryCodeRepository.deleteByUserId(userId);
        return generateAndSaveRecoveryCodes(userId);
    }

    // ===== RFC 6238 TOTP Implementation =====

    private boolean verifyCode(String base32Secret, String code) {
        byte[] key = decodeBase32(base32Secret);
        long currentTimeStep = System.currentTimeMillis() / 1000 / period;

        for (int i = -timeStepWindow; i <= timeStepWindow; i++) {
            String generatedCode = generateTotpCode(key, currentTimeStep + i);
            if (generatedCode.equals(code)) {
                return true;
            }
        }
        return false;
    }

    private String generateTotpCode(byte[] key, long timeStep) {
        try {
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            byte[] hash = mac.doFinal(timeBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, digits);
            return String.format("%0" + digits + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("TOTP generation failed", e);
        }
    }

    // ===== Recovery Code =====

    private boolean verifyRecoveryCode(UUID userId, String code) {
        List<RecoveryCodeEntity> unusedCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(userId);
        for (RecoveryCodeEntity rc : unusedCodes) {
            if (passwordEncoder.matches(code, rc.getCodeHash())) {
                rc.setUsed(true);
                rc.setUsedAt(LocalDateTime.now());
                recoveryCodeRepository.save(rc);
                log.info("Recovery code used for userId={}", userId);
                return true;
            }
        }
        return false;
    }

    private List<String> generateAndSaveRecoveryCodes(UUID userId) {
        recoveryCodeRepository.deleteByUserId(userId);

        List<String> plainCodes = new ArrayList<>();
        for (int i = 0; i < recoveryCodeCount; i++) {
            String code = generateRecoveryCode();
            plainCodes.add(code);

            RecoveryCodeEntity entity = RecoveryCodeEntity.builder()
                    .userId(userId)
                    .codeHash(passwordEncoder.encode(code))
                    .used(false)
                    .build();
            recoveryCodeRepository.save(entity);
        }
        return plainCodes;
    }

    private String generateRecoveryCode() {
        StringBuilder sb = new StringBuilder(recoveryCodeLength);
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < recoveryCodeLength; i++) {
            if (i == recoveryCodeLength / 2) {
                sb.append('-');
            }
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ===== QR Code =====

    private String buildOtpauthUri(String username, String secret) {
        String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String encodedAccount = URLEncoder.encode(issuer + ":" + username, StandardCharsets.UTF_8);
        return String.format("otpauth://totp/%s?secret=%s&issuer=%s&algorithm=%s&digits=%d&period=%d",
                encodedAccount, secret, encodedIssuer,
                algorithm.replace("Hmac", ""), digits, period);
    }

    private String generateQrCodeDataUri(String otpauthUri) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(otpauthUri, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);

            String base64 = Base64.getEncoder().encodeToString(out.toByteArray());
            return "data:image/png;base64," + base64;
        } catch (Exception e) {
            log.error("QR code generation failed", e);
            return null;
        }
    }

    // ===== Base32 =====

    private String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_CHARS.charAt((buffer >> (bitsLeft - 5)) & 0x1F));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return result.toString();
    }

    private byte[] decodeBase32(String base32) {
        String upper = base32.toUpperCase().replaceAll("[^A-Z2-7]", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int buffer = 0;
        int bitsLeft = 0;
        for (char c : upper.toCharArray()) {
            int val = BASE32_CHARS.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out.write((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return out.toByteArray();
    }
}
