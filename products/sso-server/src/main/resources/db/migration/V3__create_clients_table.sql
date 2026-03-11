-- V3: OAuth2 clients table
CREATE TABLE sso_clients (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id           VARCHAR(255) NOT NULL UNIQUE,
    client_secret_hash  VARCHAR(512),
    client_name         VARCHAR(255) NOT NULL,
    client_type         VARCHAR(32)  NOT NULL DEFAULT 'CONFIDENTIAL',
    redirect_uris       TEXT         NOT NULL DEFAULT '[]',
    allowed_scopes      TEXT         NOT NULL DEFAULT '["openid"]',
    allowed_grant_types TEXT         NOT NULL DEFAULT '["authorization_code"]',
    access_token_validity  INTEGER   NOT NULL DEFAULT 300,
    refresh_token_validity INTEGER   NOT NULL DEFAULT 86400,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    require_pkce        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_clients_client_id ON sso_clients(client_id);
