-- V7: Audit events table (immutable)
CREATE TABLE sso_audit_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type      VARCHAR(64)  NOT NULL,
    action          VARCHAR(128) NOT NULL,
    user_id         VARCHAR(256),
    client_id       VARCHAR(256),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    resource_type   VARCHAR(64),
    resource_id     VARCHAR(256),
    success         BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message   VARCHAR(1024),
    details         TEXT,
    timestamp       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_audit_event_type ON sso_audit_events(event_type);
CREATE INDEX idx_sso_audit_action ON sso_audit_events(action);
CREATE INDEX idx_sso_audit_user_id ON sso_audit_events(user_id);
CREATE INDEX idx_sso_audit_timestamp ON sso_audit_events(timestamp);

-- Prevent UPDATE/DELETE on audit events (immutability)
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit events are immutable and cannot be modified or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_no_update
    BEFORE UPDATE ON sso_audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trg_audit_no_delete
    BEFORE DELETE ON sso_audit_events
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- Login attempts table (brute force protection)
CREATE TABLE sso_login_attempts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(45)  NOT NULL,
    success     BOOLEAN      NOT NULL DEFAULT FALSE,
    attempted_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sso_login_attempts_username ON sso_login_attempts(username);
CREATE INDEX idx_sso_login_attempts_ip ON sso_login_attempts(ip_address);
CREATE INDEX idx_sso_login_attempts_time ON sso_login_attempts(attempted_at);
