# Changelog

본 문서는 AuthFusion Platform의 모든 주요 변경 사항을 기록합니다.
[Conventional Commits](https://www.conventionalcommits.org/) 규칙을 따릅니다.

## [Unreleased]

### Added
- TOTP MFA (RFC 6238) 구현 - QR 코드, 복구 코드 지원
- LDAP 사용자 저장소 연동 (search-then-bind)
- Extension Layer SPI 아키텍처 (인증/IdP/프로토콜/프로비저닝/크리덴셜)
- 권장 모노레포 구조로 재구성 (products/platform/shared/release)
- CC 문서 체계 수립 (docs/cc/)
- CI/CD 파이프라인 구조 (ci/)
- 릴리즈 인프라 (SBOM, 서명, 재현빌드)

### Changed
- 프로젝트 디렉터리 구조 재구성
  - `sso-server/` → `products/sso-server/`
  - `sso-agent/` → `products/sso-agent/`
  - `admin-console/` → `products/admin-console/`
- docker-compose.yml 빌드 컨텍스트 경로 업데이트
- UserEntity에 userSource, externalId, ldapSyncedAt 필드 추가
- LoginResponse에 mfaRequired, mfaToken 필드 추가
- AuditEventType에 MFA_OPERATION, LDAP_OPERATION 추가

## [0.1.0] - 2026-02-14

### Added
- SSO Server: OIDC Provider (Authorization Code + PKCE)
- SSO Server: JWT RSA 키 관리 (AES-256-GCM 암호화)
- SSO Server: 사용자 인증/관리 API
- SSO Server: OAuth2 클라이언트 관리
- SSO Server: RBAC 역할 관리
- SSO Server: 세션 관리
- SSO Server: 감사 로그
- SSO Server: CC 모드 (최소 TOE) 지원
- SSO Agent: Servlet Filter 기반 SSO 연동
- Admin Console: Next.js 14 관리 UI
- Docker Compose 기반 배포
