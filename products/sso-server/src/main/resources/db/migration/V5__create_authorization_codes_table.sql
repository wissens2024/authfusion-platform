-- V5: Authorization codes table (PKCE support)
CREATE TABLE sso_authorization_codes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(512) NOT NULL UNIQUE,
    client_id           VARCHAR(255) NOT NULL,
    user_id             UUID         NOT NULL REFERENCES sso_users(id),
    redirect_uri        VARCHAR(2048) NOT NULL,
    scope               VARCHAR(1024) NOT NULL,
    code_challenge      VARCHAR(256),
    code_challenge_method VARCHAR(16),
    nonce               VARCHAR(256),
    state               VARCHAR(256),
    used                BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at          TIMESTAMP    NOT NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_auth_codes_code ON sso_authorization_codes(code);
CREATE INDEX idx_sso_auth_codes_client_id ON sso_authorization_codes(client_id);
