-- V2: Password history table
CREATE TABLE sso_password_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES sso_users(id) ON DELETE CASCADE,
    password_hash   VARCHAR(512) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_password_history_user_id ON sso_password_history(user_id);
