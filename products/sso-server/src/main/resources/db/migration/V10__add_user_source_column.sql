-- ============================================================================
-- V10: LDAP 연동을 위한 사용자 소스 컬럼 추가
-- ============================================================================

ALTER TABLE sso_users ADD COLUMN user_source VARCHAR(16) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE sso_users ADD COLUMN external_id VARCHAR(512);
ALTER TABLE sso_users ADD COLUMN ldap_synced_at TIMESTAMP;

CREATE INDEX idx_sso_users_user_source ON sso_users(user_source);
CREATE INDEX idx_sso_users_external_id ON sso_users(external_id);
