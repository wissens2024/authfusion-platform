# AuthFusion Platform

## Project Overview
자체 개발 SSO Server를 중심으로 한 **AuthFusion SSO Server**와 선택형 **SSO Agent**로 구성된 IAM 솔루션입니다.
운영환경(비-TOE)은 PostgreSQL DB를 중심으로 DBMS/Directory/HSM/NTP/SIEM 등을 분리하여 관리하며, CC(Common Criteria) 평가를 빠르게 획득할 수 있는 3층 구조를 채택합니다.

## Project Structure
```
authfusion-platform/
├── products/                    # TOE 제품 코드
│   ├── sso-server/              # SSO Server - OIDC Provider (Spring Boot 3.2, Java 17) - port 8080
│   ├── sso-agent/               # SSO Agent - Servlet Filter JAR 라이브러리 (Java 17, Maven)
│   └── admin-console/           # Admin Console (Next.js 14, TypeScript) - port 3000
├── platform/                    # 비-TOE 확장 모듈 (향후)
│   ├── keyhub/                  # 크리덴셜 관리
│   ├── connect/                 # 외부 IdP 연동
│   └── automation/              # 프로비저닝 자동화
├── shared/                      # 공통 라이브러리 (향후)
├── release/                     # 릴리즈 인프라
│   ├── build/reproducible/      # 재현 빌드 가이드
│   ├── sbom/                    # SBOM 생성/검증 스크립트
│   ├── signing/                 # GPG 서명/검증 스크립트
│   ├── versioning/              # VERSION, CHANGELOG
│   └── artifacts/               # 빌드 아티팩트 출력
├── ci/                          # CI/CD
│   ├── pipelines/               # GitHub Actions (build, security-scan, release)
│   └── policies/                # 브랜치 보호, LTS 정책
├── tools/                       # 개발/CC 도구
│   ├── cc/                      # CC 설정 린터, TOE diff
│   └── dev/                     # 개발 환경 설정
├── docs/                        # 문서
│   ├── architecture/            # 아키텍처 설계 문서
│   ├── cc/                      # CC 인증 문서 (TOE 경계, ST, AGD 등)
│   ├── common/                  # 공통 문서 (배포, OIDC 플로우)
│   ├── extended/                # 확장 기능 문서
│   └── ops/                     # 운영 가이드 (보안 강화, 에어갭, 업그레이드)
├── docker-compose.yml           # Full stack: SSO Server(8080), PostgreSQL(5432), Vault(8200)
├── docker-compose.cc.yml        # CC 모드 오버라이드
└── config/                      # 공유 설정
```

## Tech Stack
- **SSO Server**: Spring Boot 3.2.5, Java 17, PostgreSQL 16, Nimbus JOSE+JWT, Flyway, Thymeleaf
- **SSO Agent**: Jakarta Servlet Filter, Nimbus JOSE+JWT, Spring Boot Auto-Configuration
- **Admin Console**: Next.js 14, React 18, TypeScript, Tailwind CSS
- **Security**: HashiCorp Vault 1.17, Thales Luna HSM (PKCS#11)
- **Infra**: Docker Compose, Nginx (reverse proxy)

## Build Commands
- `sso-server`: `cd products/sso-server && mvn clean package`
- `sso-server (CC)`: `cd products/sso-server && mvn clean package -Pcc`
- `sso-agent`: `cd products/sso-agent && mvn clean package`
- `admin-console`: `cd products/admin-console && npm install && npm run dev`
- Full stack: `docker compose up -d`
- CC stack: `docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d`

## Key Design Decisions
- SSO Server는 자체 OIDC Provider (Authorization Code + PKCE, Client Credentials, Refresh Token)
- JWT 서명키는 RSA 키 페어로 자체 관리 (KeyPairManager)
- RSA Private Key는 AES-256-GCM으로 암호화 저장 (KeyEncryptionService)
- Admin Console은 SSO Server API를 통해 데이터 접근 (직접 DB 접근 없음)
- API 인증은 Bearer Token (JWT) 방식
- SSO Agent는 Servlet Filter 기반 JAR 라이브러리로 레거시 앱 연동 지원
- TOTP MFA (RFC 6238) 지원 - HMAC-SHA1, QR 코드, 복구 코드
- LDAP 연동 (search-then-bind) - 조건부 활성화
- Extension Layer (SPI) - CC 모드에서 비활성화

## Recommended Architecture & TOE Guidelines
- **3층 구조**: AuthFusion SSO Server(TOE) + 선택형 SSO Agent(TOE) + 외부 운영환경(비-TOE PostgreSQL DB 포함)
- **TOE 포함 구성요소 최소 세트**
  * SSO Server 런타임 (Spring Boot OIDC Provider)
  * 관리자 UI/Web 콘솔
  * 감사로그 생성·보관 기능
  * 토큰/세션/정책 결정 로직
  * MFA (TOTP) 인증 기능
  * (선택) 레거시 앱용 SSO Agent – Servlet Filter 기반
- **운영환경(비-TOE)**
  * DBMS (PostgreSQL) – 제품 자체를 TOE로 만들지 않음
  * 디렉터리(AD/LDAP) – 외부 사용자 저장소
  * HSM/암호모듈 – 키는 서버가 직접 보관하지 않고 사용
  * NTP/SIEM/모니터링 – 감사로그 장기 보관/분석
- **설계 원칙 요약**
  1. OIDC 중심 프로토콜 (Authorization Code + PKCE)
  2. CC 모드(하드닝) 기본 제공 배포물
  3. SBOM·재현 빌드·서명된 배포물
  4. 장기지원(LTS) 및 백포트 정책 유지

이 지침은 CC 평가를 효율화하면서 제품을 경량화하고, 운영환경에서 차별화를 추구하기 위한 전략을 담고 있습니다.

## TOE Boundary Enforcement
- **디렉토리 수준**: `products/` = TOE, `platform/` = 비-TOE
- **어노테이션**: `@ToeScope` (TOE 코드), `@ExtendedFeature` (비-TOE 코드)
- **조건부 활성화**: `@ConditionalOnExtendedMode` (CC 모드에서 비활성화)
- **CC 설정**: `authfusion.sso.cc.extended-features-enabled=false`
- **CI 검증**: `tools/cc/toe-diff.sh` (TOE 변경 감지), `tools/cc/config-linter.sh` (CC 설정 검증)

## Conventions
- Java: Google Java Style, Lombok 사용
- TypeScript: strict mode, path alias `@/*`
- API: RESTful, `/api/v1/` prefix, JSON request/response
- Documentation: Korean (한국어)
- Git: conventional commits

## Flyway Migrations
- V1-V7: 기본 스키마 (users, clients, roles, sessions, audit, consent, passwords)
- V8: `sso_signing_keys` (암호화된 키 저장)
- V9: MFA 테이블 (totp_secrets, recovery_codes, mfa_pending_sessions)
- V10: 사용자 소스 컬럼 (user_source, external_id, ldap_synced_at)

## Important Endpoints (SSO Server)
- `GET /.well-known/openid-configuration` - OIDC Discovery
- `GET /.well-known/jwks.json` - JWKS 공개키 세트
- `GET/POST /oauth2/authorize` - 인가 엔드포인트 (Authorization Code + PKCE)
- `POST /oauth2/token` - 토큰 엔드포인트
- `GET /oauth2/userinfo` - 사용자 정보
- `POST /oauth2/revoke` - 토큰 폐기
- `POST/GET /api/v1/users` - 사용자 관리
- `POST /api/v1/auth/login` - 로그인
- `POST /api/v1/auth/mfa/verify` - MFA 검증
- `POST/GET /api/v1/clients` - OAuth2 클라이언트 관리
- `POST/GET /api/v1/roles` - 역할(RBAC) 관리
- `GET /api/v1/sessions` - 세션 관리
- `GET /api/v1/audit/events` - 감사 로그 조회
- `POST/GET /api/v1/mfa/*` - MFA 관리
- `GET /api/v1/extensions` - 확장 모듈 (Extended 모드)
- Swagger UI: http://localhost:8081/swagger-ui.html
