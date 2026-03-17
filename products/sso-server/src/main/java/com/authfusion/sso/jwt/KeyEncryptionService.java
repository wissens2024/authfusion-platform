package com.authfusion.sso.jwt;

import com.authfusion.sso.cc.ToeScope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM을 사용하여 RSA Private Key를 암호화/복호화합니다.
 * 마스터 키는 환경변수(AUTHFUSION_KEY_MASTER_SECRET)에서 주입합니다.
 *
 * <p>HSM 사용 불가 시 DB에 비밀키를 저장하기 위한 소프트웨어 기반 보호 계층입니다.
 * 향후 HSM 전환 시 이 서비스를 HSM 인터페이스로 교체하면 됩니다.</p>
 */
@ToeScope(value = "서명키 암호화 서비스", sfr = {"FCS_COP.1", "FCS_CKM.1", "FCS_CKM.4"})
@Service
@Slf4j
public class KeyEncryptionService {

    private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final SecretKey masterKey;

    public KeyEncryptionService(
            @Value("${authfusion.sso.jwt.master-secret:#{null}}") String masterSecret,
            @Value("${authfusion.sso.cc.extended-features-enabled:true}") boolean extendedFeaturesEnabled) {
        boolean ccMode = !extendedFeaturesEnabled;
        if (masterSecret == null || masterSecret.isBlank()) {
            masterSecret = System.getenv("AUTHFUSION_KEY_MASTER_SECRET");
        }
        if (masterSecret == null || masterSecret.isBlank()) {
            if (ccMode) {
                throw new IllegalStateException(
                        "CC 모드에서는 AUTHFUSION_KEY_MASTER_SECRET 환경변수를 반드시 설정해야 합니다. "
                        + "기본 마스터 키 사용이 허용되지 않습니다.");
            }
            log.warn("⚠ AUTHFUSION_KEY_MASTER_SECRET 미설정! 개발용 기본 키를 사용합니다. "
                    + "운영 환경에서는 반드시 설정하세요.");
            masterSecret = "authfusion-default-master-key-change-me-in-production";
        }
        this.masterKey = deriveKey(masterSecret);
        log.info("KeyEncryptionService 초기화 완료 (AES-256-GCM)");
    }

    /**
     * 평문 비밀키(PEM)를 AES-256-GCM으로 암호화합니다.
     *
     * @return [encryptedBase64, ivBase64] 배열
     */
    public String[] encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return new String[]{
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv)
            };
        } catch (Exception e) {
            throw new IllegalStateException("키 암호화 실패", e);
        }
    }

    /**
     * AES-256-GCM으로 암호화된 비밀키를 복호화합니다.
     */
    public String decrypt(String encryptedBase64, String ivBase64) {
        try {
            byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
            byte[] iv = Base64.getDecoder().decode(ivBase64);

            Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("키 복호화 실패", e);
        }
    }

    /**
     * 마스터 시크릿 문자열을 SHA-256으로 해시하여 256비트 AES 키를 파생합니다.
     */
    private SecretKey deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("마스터 키 파생 실패", e);
        }
    }
}
