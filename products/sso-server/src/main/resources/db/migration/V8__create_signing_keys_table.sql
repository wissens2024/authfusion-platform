-- ============================================================================
-- V8: JWT 서명 키 영속화 테이블
-- RSA Private Key는 AES-256-GCM으로 암호화하여 저장
-- ============================================================================

CREATE TABLE sso_signing_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kid             VARCHAR(255) NOT NULL UNIQUE,
    algorithm       VARCHAR(32)  NOT NULL DEFAULT 'RS256',
    key_size        INTEGER      NOT NULL DEFAULT 2048,
    public_key      TEXT         NOT NULL,               -- PEM 형식 공개키
    encrypted_private_key TEXT   NOT NULL,               -- AES-256-GCM 암호화된 비밀키 (Base64)
    iv              VARCHAR(64)  NOT NULL,               -- AES-GCM 초기화 벡터 (Base64)
    active          BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    rotated_at      TIMESTAMP
);

CREATE INDEX idx_signing_keys_active ON sso_signing_keys (active) WHERE active = TRUE;
CREATE INDEX idx_signing_keys_kid ON sso_signing_keys (kid);

COMMENT ON TABLE sso_signing_keys IS 'JWT 서명용 RSA 키 페어 (비밀키는 AES-256-GCM 암호화)';
COMMENT ON COLUMN sso_signing_keys.encrypted_private_key IS 'AUTHFUSION_KEY_MASTER_SECRET 환경변수로 암호화된 비밀키';
COMMENT ON COLUMN sso_signing_keys.iv IS 'AES-GCM에 사용된 초기화 벡터';
