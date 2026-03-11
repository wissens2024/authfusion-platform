-- V6: Refresh tokens table
CREATE TABLE sso_refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_hash  VARCHAR(512) NOT NULL UNIQUE,
    client_id   VARCHAR(255) NOT NULL,
    user_id     UUID         REFERENCES sso_users(id),
    scope       VARCHAR(1024) NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_refresh_tokens_hash ON sso_refresh_tokens(token_hash);
CREATE INDEX idx_sso_refresh_tokens_client_id ON sso_refresh_tokens(client_id);
CREATE INDEX idx_sso_refresh_tokens_user_id ON sso_refresh_tokens(user_id);
