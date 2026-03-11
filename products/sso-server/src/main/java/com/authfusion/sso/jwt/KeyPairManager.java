package com.authfusion.sso.jwt;

import com.authfusion.sso.cc.ToeScope;
import com.authfusion.sso.jwt.entity.SigningKeyEntity;
import com.authfusion.sso.jwt.repository.SigningKeyRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RSA 키 페어 관리자.
 * 키를 PostgreSQL DB에 AES-256-GCM으로 암호화하여 영속화합니다.
 *
 * <p>서버 재시작 시에도 기존 키가 유지되므로 기발급 JWT가 무효화되지 않습니다.
 * 복수 키를 보관하여 키 로테이션 시 이전 키로 서명된 토큰도 검증 가능합니다.</p>
 */
@ToeScope(value = "RSA 키 페어 관리", sfr = {"FCS_CKM.1", "FCS_CKM.2", "FCS_CKM.4"})
@Component
@Slf4j
public class KeyPairManager {

    @Value("${authfusion.sso.jwt.key-size:2048}")
    private int keySize;

    @Value("${authfusion.sso.jwt.key-rotation-days:90}")
    private int keyRotationDays;

    private final SigningKeyRepository signingKeyRepository;
    private final KeyEncryptionService keyEncryptionService;

    @Getter
    private KeyPair activeKeyPair;

    @Getter
    private String activeKid;

    /** kid → KeyPair 매핑 (이전 키 포함, 검증용) */
    private final Map<String, KeyPair> keyPairCache = new ConcurrentHashMap<>();

    public KeyPairManager(SigningKeyRepository signingKeyRepository,
                          KeyEncryptionService keyEncryptionService) {
        this.signingKeyRepository = signingKeyRepository;
        this.keyEncryptionService = keyEncryptionService;
    }

    @PostConstruct
    public void init() {
        Optional<SigningKeyEntity> existingActive = signingKeyRepository.findByActiveTrue();
        if (existingActive.isPresent()) {
            SigningKeyEntity entity = existingActive.get();
            loadKeyFromEntity(entity);
            log.info("DB에서 활성 서명 키 로드 완료: kid={}", activeKid);

            // 만료 확인 후 필요 시 로테이션
            if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.info("활성 키가 만료되었습니다. 키 로테이션을 수행합니다.");
                rotateKeyPair();
            }
        } else {
            log.info("DB에 활성 서명 키가 없습니다. 새 키를 생성합니다.");
            rotateKeyPair();
        }

        // 이전 키들도 캐시에 로드 (토큰 검증용)
        loadAllKeysToCache();
    }

    /**
     * 새 RSA 키 페어를 생성하고 DB에 암호화하여 저장합니다.
     * 기존 활성 키는 비활성화됩니다.
     */
    public void rotateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(keySize);
            KeyPair newKeyPair = generator.generateKeyPair();
            String newKid = UUID.randomUUID().toString();

            // 기존 활성 키 비활성화
            signingKeyRepository.findByActiveTrue().ifPresent(entity -> {
                entity.setActive(false);
                entity.setRotatedAt(LocalDateTime.now());
                signingKeyRepository.save(entity);
                log.info("이전 활성 키 비활성화: kid={}", entity.getKid());
            });

            // 비밀키 암호화 후 DB 저장
            String publicKeyPem = encodePublicKey(newKeyPair.getPublic());
            String privateKeyPem = encodePrivateKey(newKeyPair.getPrivate());
            String[] encrypted = keyEncryptionService.encrypt(privateKeyPem);

            SigningKeyEntity entity = SigningKeyEntity.builder()
                    .kid(newKid)
                    .algorithm("RS256")
                    .keySize(keySize)
                    .publicKey(publicKeyPem)
                    .encryptedPrivateKey(encrypted[0])
                    .iv(encrypted[1])
                    .active(true)
                    .expiresAt(LocalDateTime.now().plusDays(keyRotationDays))
                    .build();

            signingKeyRepository.save(entity);

            this.activeKeyPair = newKeyPair;
            this.activeKid = newKid;
            this.keyPairCache.put(newKid, newKeyPair);

            log.info("새 RSA 키 페어 생성 및 DB 저장 완료: kid={}, keySize={}", newKid, keySize);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA 알고리즘을 사용할 수 없습니다", e);
        }
    }

    public RSAPublicKey getPublicKey() {
        return (RSAPublicKey) activeKeyPair.getPublic();
    }

    public RSAPrivateKey getPrivateKey() {
        return (RSAPrivateKey) activeKeyPair.getPrivate();
    }

    /**
     * kid로 공개키를 조회합니다 (토큰 검증 시 사용).
     * 활성 키뿐 아니라 이전 키도 포함합니다.
     */
    public Optional<RSAPublicKey> getPublicKeyByKid(String kid) {
        KeyPair pair = keyPairCache.get(kid);
        if (pair != null) {
            return Optional.of((RSAPublicKey) pair.getPublic());
        }
        // 캐시에 없으면 DB에서 로드 시도
        return signingKeyRepository.findByKid(kid)
                .map(entity -> {
                    loadKeyFromEntity(entity);
                    return (RSAPublicKey) keyPairCache.get(kid).getPublic();
                });
    }

    /**
     * 모든 키의 공개키 목록을 반환합니다 (JWKS 엔드포인트용).
     */
    public Map<String, RSAPublicKey> getAllPublicKeys() {
        Map<String, RSAPublicKey> result = new LinkedHashMap<>();
        for (Map.Entry<String, KeyPair> entry : keyPairCache.entrySet()) {
            result.put(entry.getKey(), (RSAPublicKey) entry.getValue().getPublic());
        }
        return result;
    }

    private void loadKeyFromEntity(SigningKeyEntity entity) {
        try {
            PublicKey publicKey = decodePublicKey(entity.getPublicKey());
            String privateKeyPem = keyEncryptionService.decrypt(
                    entity.getEncryptedPrivateKey(), entity.getIv());
            PrivateKey privateKey = decodePrivateKey(privateKeyPem);

            KeyPair keyPair = new KeyPair(publicKey, privateKey);
            this.keyPairCache.put(entity.getKid(), keyPair);

            if (entity.isActive()) {
                this.activeKeyPair = keyPair;
                this.activeKid = entity.getKid();
            }
        } catch (Exception e) {
            log.error("DB에서 키 로드 실패: kid={}", entity.getKid(), e);
            throw new IllegalStateException("서명 키 로드 실패", e);
        }
    }

    private void loadAllKeysToCache() {
        List<SigningKeyEntity> allKeys = signingKeyRepository.findAllByOrderByCreatedAtDesc();
        for (SigningKeyEntity entity : allKeys) {
            if (!keyPairCache.containsKey(entity.getKid())) {
                try {
                    loadKeyFromEntity(entity);
                } catch (Exception e) {
                    log.warn("이전 키 로드 실패 (무시): kid={}", entity.getKid(), e);
                }
            }
        }
        log.info("총 {}개의 서명 키를 캐시에 로드했습니다.", keyPairCache.size());
    }

    private String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    private String encodePrivateKey(PrivateKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    private PublicKey decodePublicKey(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("공개키 디코딩 실패", e);
        }
    }

    private PrivateKey decodePrivateKey(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("비밀키 디코딩 실패", e);
        }
    }
}
