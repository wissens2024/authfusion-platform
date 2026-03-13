-- V11: 감사 로그에 username 컬럼 추가 (조회 편의성)
ALTER TABLE sso.sso_audit_events ADD COLUMN IF NOT EXISTS username VARCHAR(255);
CREATE INDEX IF NOT EXISTS idx_sso_audit_username ON sso.sso_audit_events(username);
