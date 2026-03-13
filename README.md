# AuthFusion Platform

> **AuthFusion SSO Server** 중심의 통합 인증 플랫폼. 자체 개발 OIDC Provider인 SSO Server와 선택형 SSO Agent로 구성되며, 운영환경(비-TOE)을 PostgreSQL DB 등으로 분리하는 3층 구조를 취합니다.

## 프로젝트 개요

AuthFusion Platform은 엔터프라이즈급 **IAM(Identity and Access Management)** 솔루션입니다.
Spring Boot 기반 자체 OIDC Provider인 **SSO Server**를 중심으로 **OAuth 2.0 / OpenID Connect** 인증을 제공하며,
**TOTP MFA**, **LDAP 연동**, **HashiCorp Vault** 연동, **HSM(Thales Luna)** 지원을 통합하여 보안 수준을 극대화한 플랫폼입니다.

### 핵심 기능

- **OIDC Provider**: Authorization Code + PKCE, Client Credentials, Refresh Token 지원
- **TOTP MFA**: RFC 6238 기반 다중 인증 (QR 코드, 복구 코드)
- **LDAP 연동**: Search-then-bind 인증, 사용자 자동 동기화
- **사용자 관리**: 사용자 CRUD, 비밀번호 정책, 비밀번호 이력 관리
- **클라이언트 관리**: OAuth2 클라이언트 등록/관리
- **RBAC**: 역할 기반 접근 제어 (역할 할당/해제)
- **세션 관리**: SSO 세션 목록/강제종료
- **감사 로그**: 불변 감사 이벤트 기록 및 조회
- **Brute Force 보호**: 로그인 시도 제한 및 계정 잠금
- **SSO Agent**: Servlet Filter 기반 JAR 라이브러리로 레거시 앱 SSO 연동
- **Extension Layer**: SPI 기반 확장 아키텍처 (FIDO2, SAML, SCIM 등 향후 지원)

---

## 아키텍처

AuthFusion 플랫폼은 CC 평가를 염두에 둔 **TOE/비-TOE 3층 구조**를 채택합니다.

```
┌─────────────────────────────────────────────────────────┐
│                    TOE (평가 대상)                        │
│  ┌──────────────────┐  ┌──────────────────┐             │
│  │  SSO Server       │  │  SSO Agent        │            │
│  │  (OIDC Provider)  │  │  (Servlet Filter) │            │
│  │  - 인증/인가       │  │  - 토큰 검증       │            │
│  │  - MFA (TOTP)     │  │  - 세션 관리       │            │
│  │  - 감사 로그       │  │  - 헤더 매핑       │            │
│  │  - Admin Console  │  │                   │            │
│  └──────────────────┘  └──────────────────┘             │
└─────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────┐
│                  운영환경 (비-TOE)                        │
│  PostgreSQL │ LDAP/AD │ HSM │ NTP │ SIEM │ Nginx       │
└─────────────────────────────────────────────────────────┘
```

---

## 기술 스택

| 구분 | 기술 | 버전 | 설명 |
|------|------|------|------|
| **SSO Server** | Spring Boot | 3.2.5 | OIDC Provider (OAuth2, JWT) |
| **JWT** | Nimbus JOSE+JWT | 9.37.3 | JWT 생성/검증 (RS256) |
| **MFA** | TOTP (RFC 6238) | - | HMAC-SHA1, ZXing QR |
| **DB Migration** | Flyway | - | PostgreSQL 스키마 마이그레이션 |
| **SSO Agent** | Jakarta Servlet | 6.0+ | Servlet Filter 기반 SSO 라이브러리 |
| **Frontend** | Next.js | 14.x | 관리자 콘솔 웹 애플리케이션 |
| **Database** | PostgreSQL | 16 | SSO 메타데이터 저장소 |
| **Secrets** | HashiCorp Vault | 1.17 | 시크릿 관리 |
| **HSM** | Thales Luna | 7.x | 하드웨어 보안 모듈 (PKCS#11) |
| **컨테이너** | Docker / Compose | - | 개발/운영 환경 구성 |

---

## 프로젝트 구조

```
authfusion-platform/
├── products/                    # TOE 제품 코드
│   ├── sso-server/              # SSO Server - OIDC Provider (Spring Boot 3.2)
│   ├── sso-agent/               # SSO Agent - Servlet Filter JAR
│   └── admin-console/           # Admin Console (Next.js 14)
├── platform/                    # 비-TOE 확장 모듈 (향후)
│   ├── keyhub/                  # 크리덴셜 관리
│   ├── connect/                 # 외부 IdP 연동
│   └── automation/              # 프로비저닝 자동화
├── shared/                      # 공통 라이브러리 (향후)
├── release/                     # 릴리즈 인프라
│   ├── build/reproducible/      # 재현 빌드 가이드
│   ├── sbom/                    # SBOM 생성/검증
│   ├── signing/                 # GPG 서명/검증
│   └── versioning/              # VERSION, CHANGELOG
├── ci/                          # CI/CD 파이프라인
│   ├── pipelines/               # build, security-scan, release
│   └── policies/                # 브랜치 보호, LTS 정책
├── tools/                       # 개발/CC 도구
│   ├── cc/                      # TOE diff, CC 설정 린터
│   └── dev/                     # 개발 환경 설정
├── docs/                        # 문서
│   ├── architecture/            # 아키텍처 설계
│   ├── cc/                      # CC 인증 문서
│   ├── common/                  # 배포, OIDC 플로우
│   ├── extended/                # 확장 기능 가이드
│   └── ops/                     # 운영 가이드
├── docker-compose.yml           # 전체 서비스 오케스트레이션
└── docker-compose.cc.yml        # CC 모드 오버라이드
```

---

## 빠른 시작 (Quick Start)

### 사전 요구사항

- **Docker** 24.0 이상
- **Docker Compose** v2 이상
- **Java** 17 이상 (SSO Server 및 SSO Agent 빌드 시)
- **Node.js** 20 이상 (Admin Console 빌드 시)
- **Maven** 3.9 이상

### 1. 저장소 클론

```bash
git clone https://github.com/your-org/authfusion-platform.git
cd authfusion-platform
```

### 2. 서브 프로젝트 빌드

```bash
# SSO Server 빌드
cd products/sso-server
mvn clean package -DskipTests
cd ../..

# SSO Agent 빌드
cd products/sso-agent
mvn clean package -DskipTests
cd ../..

# Admin Console 빌드
cd products/admin-console
npm install
npm run build
cd ../..
```

### 3. Docker Compose 실행

```bash
# 전체 서비스 시작
docker compose up -d

# CC 모드 시작
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d

# 로그 확인
docker compose logs -f sso-server
```

### 4. 서비스 접속

| 서비스 | URL | 설명 |
|--------|-----|------|
| SSO Server (Swagger) | http://localhost:8081/swagger-ui.html | REST API 문서 |
| SSO Server 로그인 | http://localhost:8081/login | SSO 로그인 페이지 |
| OIDC Discovery | http://localhost:8081/.well-known/openid-configuration | OIDC 메타데이터 |
| Admin Console | http://localhost:3001 | 관리자 콘솔 |
| Vault UI | http://localhost:8200 | Token: authfusion-dev-token |

### 5. 서비스 종료

```bash
docker compose down       # 서비스 중지
docker compose down -v    # 볼륨 포함 완전 삭제
```

---

## 서브 프로젝트 설명

### products/sso-server/ (AuthFusion SSO Server)

Spring Boot 기반 자체 OIDC Provider입니다.

- **OIDC 엔드포인트**: Discovery, JWKS, Authorization, Token, UserInfo, Revocation
- **사용자 관리**: CRUD, 비밀번호 정책, 계정 잠금
- **TOTP MFA**: RFC 6238 기반 다중 인증 (QR 코드, 복구 코드)
- **LDAP 연동**: Search-then-bind 인증, 사용자 동기화
- **클라이언트 관리**: OAuth2 클라이언트 등록/관리
- **RBAC**: 역할 기반 접근 제어
- **세션 관리**: SSO 세션 관리 및 강제 종료
- **감사 로그**: 불변 감사 이벤트 기록
- **Extension Layer**: SPI 기반 확장 아키텍처

### products/sso-agent/ (AuthFusion SSO Agent)

레거시 앱 연동을 위한 Servlet Filter 기반 JAR 라이브러리입니다.

- **SsoAuthenticationFilter**: 미인증 요청을 SSO Server로 리다이렉트
- **SsoAuthorizationFilter**: URL 패턴 + 역할 기반 접근 제어
- **SsoLogoutFilter**: 로컬 세션 + SSO Server Single Logout
- **Spring Boot Auto-Configuration**: `@EnableSsoAgent`로 자동 설정

### products/admin-console/ (Admin Console UI)

관리자용 웹 콘솔로 사용자, 클라이언트, 역할, 세션, 감사 로그를 관리합니다.

- **대시보드**: 인증 통계, 시스템 상태 모니터링
- **사용자 관리**: 사용자/역할/권한 관리, MFA 상태 확인
- **LDAP 현황**: 사용자 소스 (Local/LDAP) 구분
- **감사 로그**: 전체 작업 이력 조회

---

## 개발 환경 설정

```bash
# 개발 환경 자동 설정
bash tools/dev/setup.sh

# 또는 수동 설정
cd products/admin-console && npm install
```

### 환경 변수 설정

```bash
# 필수
export AUTHFUSION_KEY_MASTER_SECRET=<마스터 시크릿>

# 선택 (LDAP 활성화 시)
export AUTHFUSION_LDAP_BIND_PASSWORD=<LDAP 비밀번호>
```

### 인프라 서비스만 실행 (로컬 개발 시)

```bash
docker compose up -d postgres vault
```

---

## 문서

| 문서 | 위치 | 설명 |
|------|------|------|
| 아키텍처 개요 | `docs/architecture/overview.md` | 전체 시스템 아키텍처 |
| Identity Engine | `docs/architecture/identity-engine.md` | 인증 엔진 상세 |
| Token Service | `docs/architecture/token-service.md` | 토큰/키 관리 |
| 감사 로깅 | `docs/architecture/audit-logging.md` | 감사 서브시스템 |
| 관리 평면 | `docs/architecture/admin-plane.md` | Admin API/UI |
| TOE 경계 | `docs/cc/toe-boundary.md` | CC TOE 경계 정의 |
| 보안 강화 | `docs/ops/hardening-guide.md` | 보안 강화 가이드 |
| 에어갭 설치 | `docs/ops/airgap-install.md` | 폐쇄망 설치 |
| 업그레이드 | `docs/ops/upgrade-rollback.md` | 업그레이드/롤백 |

---

## 라이선스

Copyright 2026 Wissens2024. All rights reserved.

이 프로젝트의 애플리케이션 코드는 Wissens2024에 의해 개발되었습니다.
사용된 오픈소스 라이브러리는 각각의 라이선스(Apache 2.0, MIT 등)를 따릅니다.
