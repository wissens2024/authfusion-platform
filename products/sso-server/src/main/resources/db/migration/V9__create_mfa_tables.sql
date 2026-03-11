-- ============================================================================
-- V9: MFA (Multi-Factor Authentication) 테이블 생성
-- TOTP 비밀키, 복구 코드, MFA 대기 세션
-- ============================================================================

-- TOTP 비밀키 (AES-256-GCM 암호화 저장)
CREATE TABLE sso_totp_secrets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES sso_users(id) ON DELETE CASCADE,
    encrypted_secret TEXT NOT NULL,
    iv              VARCHAR(64) NOT NULL,
    algorithm       VARCHAR(16) NOT NULL DEFAULT 'HmacSHA1',
    digits          INT NOT NULL DEFAULT 6,
    period          INT NOT NULL DEFAULT 30,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    CONSTRAINT uq_totp_secrets_user_id UNIQUE (user_id)
);

CREATE INDEX idx_totp_secrets_user_id ON sso_totp_secrets(user_id);

-- 복구 코드 (BCrypt 해시 저장)
CREATE TABLE sso_recovery_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES sso_users(id) ON DELETE CASCADE,
    code_hash       VARCHAR(512) NOT NULL,
    used            BOOLEAN NOT NULL DEFAULT FALSE,
    used_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_recovery_codes_user_id ON sso_recovery_codes(user_id);

-- MFA 대기 세션 (OAuth2 플로우 컨텍스트 포함)
CREATE TABLE sso_mfa_pending_sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mfa_token               VARCHAR(512) NOT NULL UNIQUE,
    user_id                 UUID NOT NULL REFERENCES sso_users(id) ON DELETE CASCADE,
    ip_address              VARCHAR(64),
    user_agent              TEXT,
    client_id               VARCHAR(255),
    redirect_uri            TEXT,
    scope                   VARCHAR(512),
    response_type           VARCHAR(64),
    state                   VARCHAR(512),
    nonce                   VARCHAR(512),
    code_challenge          VARCHAR(512),
    code_challenge_method   VARCHAR(16),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at              TIMESTAMP NOT NULL
);

CREATE INDEX idx_mfa_pending_mfa_token ON sso_mfa_pending_sessions(mfa_token);
CREATE INDEX idx_mfa_pending_user_id ON sso_mfa_pending_sessions(user_id);
CREATE INDEX idx_mfa_pending_expires_at ON sso_mfa_pending_sessions(expires_at);
