-- ============================================================================
-- AuthFusion SSO Server - DB 초기화 스크립트
-- 실행: postgres 사용자로 접속하여 실행
--   psql -h 192.168.0.115 -U postgres -f init-sso-database.sql
-- ============================================================================

-- 1. SSO 전용 사용자 생성
DROP USER IF EXISTS sso_user;
CREATE USER sso_user WITH LOGIN PASSWORD 'AuthFusion2026!';

-- 2. SSO 전용 DB 생성
-- CREATE DATABASE authfusion_db OWNER sso_user ENCODING 'UTF8';
-- (이미 생성된 경우 위 줄은 주석 처리)

-- 3. DB 권한
GRANT ALL PRIVILEGES ON DATABASE authfusion_db TO sso_user;

-- 4. authfusion_db 접속 후 스키마 설정
\c authfusion_db

CREATE SCHEMA IF NOT EXISTS sso AUTHORIZATION sso_user;
GRANT ALL ON SCHEMA sso TO sso_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA sso GRANT ALL ON TABLES TO sso_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA sso GRANT ALL ON SEQUENCES TO sso_user;
ALTER USER sso_user SET search_path TO sso;
REVOKE CREATE ON SCHEMA public FROM sso_user;

-- 5. sse_db 정리 (이전 sso 스키마 제거)
\c sse_db
DROP SCHEMA IF EXISTS sso CASCADE;

\echo '=== 완료: authfusion_db.sso 스키마 준비됨 ==='
