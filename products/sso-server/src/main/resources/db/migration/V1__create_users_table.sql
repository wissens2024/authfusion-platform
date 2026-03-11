-- V1: Users table
CREATE TABLE sso_users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(512) NOT NULL,
    first_name      VARCHAR(255),
    last_name       VARCHAR(255),
    status          VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    login_fail_count INTEGER     NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    last_login_at   TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_users_username ON sso_users(username);
CREATE INDEX idx_sso_users_email ON sso_users(email);
CREATE INDEX idx_sso_users_status ON sso_users(status);
