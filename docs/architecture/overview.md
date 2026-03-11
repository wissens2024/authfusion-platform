# 아키텍처 개요 (Architecture Overview)

## AuthFusion SSO Platform v1.0

---

## 1. 개요

AuthFusion SSO Platform은 OIDC(OpenID Connect) 기반의 통합 인증(IAM) 솔루션이다.
자체 개발한 SSO Server를 중심으로, 선택형 SSO Agent와 관리 콘솔로 구성되며,
CC(Common Criteria) 인증을 효율적으로 획득할 수 있는 3층 구조를 채택한다.

### 1.1 설계 원칙

1. **OIDC 중심 프로토콜**: Authorization Code + PKCE를 핵심 인증 플로우로 채택
2. **CC 모드 기본 제공**: 최소 TOE 배포물을 기본 제공하여 평가 범위 최소화
3. **확장 가능 아키텍처**: SPI(Service Provider Interface) 기반 확장 레이어로 FIDO2, SAML, SCIM 등 지원 가능
4. **운영환경 분리**: DBMS, HSM, 디렉터리 등을 운영환경으로 분리하여 TOE 경량화
5. **SBOM 및 재현 빌드**: 소프트웨어 공급망 보안 확보

### 1.2 기술 스택 요약

| 계층 | 기술 | 버전 |
|------|------|------|
| SSO Server | Spring Boot, Java | 3.2.5, Java 17 |
| SSO Agent | Jakarta Servlet Filter, Java | Java 17 |
| Admin Console | Next.js, React, TypeScript | 14, 18 |
| 데이터베이스 | PostgreSQL | 16 |
| JWT 라이브러리 | Nimbus JOSE+JWT | - |
| 마이그레이션 | Flyway | - |
| 템플릿 엔진 | Thymeleaf | - |
| 시크릿 관리 | HashiCorp Vault | 1.17 |
| HSM | Thales Luna (PKCS#11) | 선택 |
| 컨테이너 | Docker Compose | - |
| 리버스 프록시 | Nginx | - |

---

## 2. 프로젝트 디렉터리 구조

```
authfusion-platform/
├── products/                        # 제품 배포물
│   ├── sso-server/                  # SSO Server (TOE) - Spring Boot 3.2, Java 17
│   │   ├── src/main/java/com/authfusion/sso/
│   │   │   ├── oidc/                # OIDC Provider 핵심
│   │   │   ├── jwt/                 # JWT 토큰/키 관리
│   │   │   ├── user/                # 사용자/인증
│   │   │   ├── mfa/                 # 다중 인증 (TOTP)
│   │   │   ├── session/             # 세션 관리
│   │   │   ├── audit/               # 감사 로그
│   │   │   ├── security/            # 보안 필터/보호
│   │   │   ├── rbac/                # 역할 기반 접근 제어
│   │   │   ├── client/              # OAuth2 클라이언트
│   │   │   ├── config/              # Spring 구성
│   │   │   ├── cc/                  # CC 어노테이션
│   │   │   └── web/                 # 웹 컨트롤러
│   │   └── src/main/resources/
│   │       ├── application.yml      # 기본 설정
│   │       ├── application-cc.yml   # CC 하드닝 프로파일
│   │       ├── application-docker.yml
│   │       └── db/migration/        # Flyway 마이그레이션 (V1~V10)
│   ├── sso-agent/                   # SSO Agent (TOE) - Servlet Filter JAR
│   │   └── src/main/java/com/authfusion/agent/
│   │       ├── filter/              # Servlet Filter 체인
│   │       ├── token/               # JWT 검증, JWKS 리졸버
│   │       ├── session/             # 세션 동기화
│   │       ├── access/              # 접근 제어
│   │       ├── context/             # 보안 컨텍스트
│   │       ├── config/              # 자동 구성
│   │       └── cc/                  # CC 어노테이션
│   └── admin-console/               # Admin Console (Non-TOE) - Next.js 14
├── platform/                        # Non-TOE 확장 기능
│   ├── keyhub/                      # 키 관리 허브
│   ├── connect/                     # 외부 시스템 연동 커넥터
│   └── automation/                  # 자동화 워크플로
├── shared/                          # 공유 라이브러리
│   ├── config/                      # 공유 설정
│   ├── contracts/                   # API 계약
│   ├── crypto/                      # 공용 암호 유틸리티
│   ├── logging/                     # 공용 로깅
│   └── utils/                       # 공용 유틸리티
├── release/                         # 릴리스 아티팩트
│   ├── artifacts/                   # 빌드 결과물, 에어갭 번들
│   ├── build/                       # 재현 가능 빌드 설정
│   ├── sbom/                        # CycloneDX SBOM
│   ├── signing/                     # GPG 서명
│   └── versioning/                  # 버전 관리
├── ci/                              # CI/CD 파이프라인
│   ├── pipelines/                   # 파이프라인 정의
│   └── policies/                    # 정책 검증
├── tools/                           # 개발/CC 도구
│   ├── cc/                          # CC 관련 도구
│   │   ├── config-linter/           # CC 설정 린터
│   │   └── toe-diff/                # TOE 변경 추적
│   └── dev/                         # 개발 도구
│       ├── local-env/               # 로컬 개발 환경
│       └── test-harness/            # 테스트 하네스
├── docs/                            # 문서
│   ├── cc/                          # CC 문서 (ST, TOE, ADV, AGD, ALC, ATE, AVA)
│   ├── architecture/                # 아키텍처 문서
│   └── ops/                         # 운영 문서
├── docker-compose.yml               # 표준 배포 (SSO Server, Admin Console, PostgreSQL, Vault)
└── docker-compose.cc.yml            # CC 모드 오버라이드
```

---

## 3. 3층 아키텍처

AuthFusion Platform은 CC 평가를 빠르게 획득할 수 있도록 3층 구조를 채택한다.

### 3.1 구조 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                     │
│                     1층: TOE (평가 대상)                             │
│                                                                     │
│  ┌─────────────────────────────────┐  ┌──────────────────────────┐  │
│  │       SSO Server (Core)         │  │     SSO Agent            │  │
│  │                                 │  │                          │  │
│  │  OIDC Provider                  │  │  Servlet Filter JAR      │  │
│  │  ├─ Authorization Endpoint      │  │  ├─ SsoAuthenticationF.  │  │
│  │  ├─ Token Endpoint              │  │  ├─ SsoAuthorizationF.   │  │
│  │  ├─ UserInfo Endpoint           │  │  ├─ SsoLogoutFilter      │  │
│  │  ├─ JWKS Endpoint               │  │  ├─ JwtTokenValidator    │  │
│  │  ├─ Revocation Endpoint         │  │  ├─ JwksKeyResolver      │  │
│  │  └─ Discovery Endpoint          │  │  ├─ AccessControlManager │  │
│  │                                 │  │  └─ SessionSyncService   │  │
│  │  Identity Engine                │  │                          │  │
│  │  ├─ AuthController              │  └──────────────────────────┘  │
│  │  ├─ UserService (BCrypt)        │                                │
│  │  ├─ BruteForceProtection        │                                │
│  │  └─ LdapAuthProvider            │                                │
│  │                                 │                                │
│  │  MFA Engine (TOTP RFC 6238)     │                                │
│  │  Token Service (RS256, JWKS)    │                                │
│  │  Key Management (AES-256-GCM)   │                                │
│  │  Audit Subsystem (FAU_GEN)      │                                │
│  │  Session Management (FTA_SSL)   │                                │
│  │  Security Config (FDP_ACC)      │                                │
│  └─────────────────────────────────┘                                │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                     2층: Non-TOE 확장                                │
│                                                                     │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐  │
│  │ Admin Console    │  │ Platform KeyHub  │  │ Platform Connect │  │
│  │ (Next.js 14)     │  │ (키 관리 허브)   │  │ (연동 커넥터)    │  │
│  └──────────────────┘  └──────────────────┘  └──────────────────┘  │
│  ┌──────────────────┐  ┌──────────────────────────────────────────┐│
│  │ Platform         │  │ Extended Controllers (@ExtendedFeature) ││
│  │ Automation       │  │ Client/User/Role/Session/AuditStats     ││
│  └──────────────────┘  └──────────────────────────────────────────┘│
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                     3층: 운영환경 (비-TOE)                           │
│                                                                     │
│  ┌─────────┐ ┌───────┐ ┌────────┐ ┌─────┐ ┌──────┐ ┌───────────┐ │
│  │PostgreSQL│ │ Vault │ │ LDAP/AD│ │ HSM │ │ NTP  │ │ SIEM      │ │
│  │ 16      │ │ 1.17  │ │        │ │Luna │ │      │ │           │ │
│  └─────────┘ └───────┘ └────────┘ └─────┘ └──────┘ └───────────┘ │
│  ┌─────────┐ ┌────────────────────────────────────────────────────┐│
│  │ Nginx   │ │ OS / JVM (Java 17, Linux)                         ││
│  └─────────┘ └────────────────────────────────────────────────────┘│
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 3.2 각 층의 역할

| 층 | 역할 | CC 분류 | 구성요소 |
|----|------|---------|---------|
| 1층 | 보안 기능 핵심 구현 | TOE | SSO Server Core, SSO Agent |
| 2층 | 관리/운영 편의 기능 | Non-TOE | Admin Console, Platform 확장, Extended Controllers |
| 3층 | 인프라/외부 서비스 | 운영환경 | PostgreSQL, Vault, LDAP, HSM, NTP, SIEM, OS/JVM |

---

## 4. 구성요소 상세

### 4.1 SSO Server (products/sso-server)

OIDC Provider로서 인증/인가의 핵심 역할을 수행한다.

**주요 모듈:**

| 모듈 | 패키지 | 역할 | SFR |
|------|--------|------|-----|
| OIDC Endpoints | `oidc.endpoint` | OAuth2/OIDC 표준 엔드포인트 6개 | FIA_UAU.1, FIA_UID.1 |
| OIDC Services | `oidc.service` | 인가 검증, PKCE, 스코프, 토큰 | FIA_UAU.1, FCS_COP.1 |
| Grant Handlers | `oidc.grant` | AuthCode(TOE), Refresh(TOE), ClientCred(EXT) | FIA_UAU.1 |
| JWT Module | `jwt` | 토큰 생성/검증, RSA 키 관리, 키 암호화 | FCS_COP.1, FCS_CKM.1/2/4 |
| Identity Engine | `user`, `security` | 사용자 인증, 비밀번호 정책, 무차별 대입 보호 | FIA_UAU.1, FIA_AFL.1, FIA_SOS.1 |
| MFA Engine | `mfa` | TOTP (RFC 6238), 복구 코드 | FIA_UAU.1, FCS_COP.1 |
| Session | `session` | SSO 세션 생성/유지/만료/강제 종료 | FIA_USB.1, FTA_SSL.3 |
| Audit | `audit` | 감사 이벤트 생성/저장/조회 | FAU_GEN.1, FAU_GEN.2 |
| RBAC | `rbac` | 역할 기반 접근 제어 | FDP_ACC.1 |
| Security Config | `config` | Spring Security 필터 체인, CORS, Rate Limit | FIA_UAU.1, FDP_ACC.1 |
| CC Annotations | `cc` | @ToeScope, @ExtendedFeature, @ConditionalOnExtendedMode | - |

**포트**: 8080 (HTTP, Nginx 뒤에서 동작)

**엔드포인트 구성:**

| 분류 | 엔드포인트 | CC 모드 |
|------|-----------|---------|
| OIDC Core | `/.well-known/*`, `/oauth2/*` | 활성 |
| Auth API | `/api/v1/auth/*` | 활성 |
| MFA API | `/api/v1/mfa/*` | 활성 |
| Audit API | `/api/v1/audit/events` | 활성 |
| Management API | `/api/v1/users,clients,roles,sessions/*` | **비활성** |
| Swagger | `/swagger-ui/*`, `/api-docs/*` | **비활성** |

### 4.2 SSO Agent (products/sso-agent)

레거시 웹 애플리케이션에 SSO 기능을 부여하는 Servlet Filter 기반 JAR 라이브러리이다.

**주요 모듈:**

| 모듈 | 패키지 | 역할 |
|------|--------|------|
| Servlet Filters | `filter` | 인증(SsoAuthenticationFilter), 인가(SsoAuthorizationFilter), 로그아웃(SsoLogoutFilter) |
| Token Validation | `token` | JWT 서명 검증(JwtTokenValidator), JWKS 키 리졸버(JwksKeyResolver), 토큰 캐시(TokenCache) |
| Access Control | `access` | URL 패턴 매칭(UrlPatternMatcher), 역할 기반 접근 검사(RoleBasedAccessChecker) |
| Session Sync | `session` | SSO Server와 세션 동기화(SessionSyncService, AgentSessionManager) |
| Security Context | `context` | 인증 정보 ThreadLocal 관리(SsoSecurityContext, SsoSecurityContextHolder) |
| Configuration | `config` | 프로퍼티(SsoAgentProperties), 자동 구성(SsoAgentAutoConfiguration), 필터 등록(SsoAgentFilterRegistrar) |
| Annotation | `annotation` | @EnableSsoAgent (활성화 어노테이션) |

**배포 형태**: JAR 라이브러리 (Maven 의존성 추가 또는 WEB-INF/lib 배치)

### 4.3 Admin Console (products/admin-console)

SSO Server의 관리 API를 통해 사용자/클라이언트/역할을 관리하는 웹 UI이다.

- **기술 스택**: Next.js 14 (App Router), React 18, TypeScript, Tailwind CSS
- **포트**: 3000
- **특징**: SSO Server API만 호출하며, DB에 직접 접근하지 않음
- **API 인증**: Bearer Token (JWT) 방식
- **CC 모드**: 비배포 (`docker-compose.cc.yml`에서 `replicas: 0`)

### 4.4 Platform 확장 (platform/*)

TOE 외부의 부가 기능을 제공한다.

| 모듈 | 하위 구조 | 역할 |
|------|----------|------|
| `platform/keyhub` | connectors, policies, service, tests, ui | SSH/API 키, 인증서 등의 자격증명 중앙 관리 허브 |
| `platform/connect` | cloud, db, firewall, linux, vpn, waf | 외부 시스템 연동 커넥터 |
| `platform/automation` | report-templates, workflows | 자동화 워크플로 및 보고서 템플릿 |

---

## 5. 데이터 흐름

### 5.1 Authorization Code + PKCE 플로우

```
 사용자           클라이언트 앱        SSO Server         PostgreSQL
  (브라우저)       (RP)                (OP, TOE)          (운영환경)
    │                │                    │                    │
    │  1. 로그인 요청 │                    │                    │
    │───────────────>│                    │                    │
    │                │ 2. /oauth2/authorize│                    │
    │                │   + code_challenge  │                    │
    │                │──────────────────>│                    │
    │                │                    │ 3. 세션 확인       │
    │                │                    │──────────────────>│
    │                │                    │<──────────────────│
    │                │                    │                    │
    │  4. 로그인 페이지 리다이렉트         │                    │
    │<──────────────────────────────────│                    │
    │                │                    │                    │
    │  5. 사용자명/비밀번호 입력          │                    │
    │──────────────────────────────────>│                    │
    │                │                    │ 6. 인증 검증       │
    │                │                    │   BCrypt / LDAP    │
    │                │                    │──────────────────>│
    │                │                    │<──────────────────│
    │                │                    │                    │
    │  7. (MFA 활성 시) TOTP 입력 요청    │                    │
    │<──────────────────────────────────│                    │
    │  8. TOTP 코드 입력                 │                    │
    │──────────────────────────────────>│                    │
    │                │                    │ 9. TOTP 검증      │
    │                │                    │   (RFC 6238)       │
    │                │                    │                    │
    │                │ 10. code + state   │                    │
    │                │   → redirect_uri   │                    │
    │<──────────────────────────────────│                    │
    │──────────────>│                    │                    │
    │                │ 11. POST /oauth2/token                 │
    │                │   + code_verifier  │                    │
    │                │──────────────────>│                    │
    │                │                    │ 12. PKCE 검증      │
    │                │                    │   + 코드 조회       │
    │                │                    │──────────────────>│
    │                │                    │<──────────────────│
    │                │                    │                    │
    │                │                    │ 13. JWT 서명       │
    │                │                    │   RS256 + kid      │
    │                │                    │                    │
    │                │ 14. access_token   │                    │
    │                │   + id_token       │                    │
    │                │   + refresh_token  │                    │
    │                │<──────────────────│                    │
    │  15. 인증 완료  │                    │                    │
    │<───────────────│                    │                    │
    │                │                    │ 16. 감사 로그 기록 │
    │                │                    │──────────────────>│
```

### 5.2 SSO Agent 토큰 검증 플로우

```
 사용자            레거시 앱 (Agent 내장)     SSO Server
  │                    │                        │
  │  1. 요청 + JWT     │                        │
  │  (Cookie/Header)   │                        │
  │───────────────────>│                        │
  │                    │                        │
  │                    │ 2. JWKS 조회 (캐시)     │
  │                    │   /.well-known/jwks.json│
  │                    │───────────────────────>│
  │                    │<───────────────────────│
  │                    │                        │
  │                    │ 3. JWT 서명 검증 (RS256)│
  │                    │   + 만료 시간 검증      │
  │                    │   + issuer 검증         │
  │                    │                        │
  │                    │ 4. URL 접근 제어 검사    │
  │                    │   (역할 기반 RBAC)      │
  │                    │                        │
  │  5. 응답 또는 로그인 리다이렉트             │
  │<───────────────────│                        │
```

### 5.3 키 관리 데이터 흐름

```
 KeyPairManager          KeyEncryptionService          PostgreSQL
    │                          │                           │
    │ 1. RSA 키 생성           │                           │
    │   (2048-bit)             │                           │
    │                          │                           │
    │ 2. 비밀키 암호화 요청     │                           │
    │─────────────────────────>│                           │
    │                          │ 3. SHA-256(masterSecret)   │
    │                          │   → AES-256 키 파생        │
    │                          │                           │
    │                          │ 4. AES-256-GCM 암호화     │
    │                          │   12-byte IV, 128-bit tag │
    │<─────────────────────────│                           │
    │                          │                           │
    │ 5. [publicKey, encryptedPrivateKey, IV] 저장          │
    │─────────────────────────────────────────────────────>│
    │                          │                           │
    │ 6. 서명 시: 캐시된 활성 키 사용                       │
    │   (메모리 내 복호화된 상태)│                           │
```

---

## 6. 데이터 저장소

### 6.1 PostgreSQL 스키마 (Flyway 마이그레이션)

| 마이그레이션 | 테이블 | 용도 | TOE 관련 |
|-------------|--------|------|---------|
| V1 | `sso_users` | 사용자 정보 (username, email, passwordHash, status) | TOE |
| V2 | `sso_password_history` | 비밀번호 이력 (재사용 방지, FIA_SOS.1) | TOE |
| V3 | `sso_clients` | OAuth2 클라이언트 등록 정보 | TOE |
| V4 | `sso_roles`, `sso_user_roles`, `sso_client_roles` | RBAC 역할 매핑 | TOE |
| V5 | `sso_authorization_codes` | 인가 코드 임시 저장 (code, PKCE, nonce) | TOE |
| V6 | `sso_refresh_tokens` | Refresh Token 저장 | TOE |
| V7 | `sso_audit_events`, `sso_login_attempts` | 감사 로그, 로그인 시도 추적 | TOE |
| V8 | `sso_signing_keys` | RSA 서명키 (암호화 저장: encryptedPrivateKey, IV, publicKey) | TOE |
| V9 | `sso_totp_secrets`, `sso_recovery_codes`, `sso_mfa_pending_sessions` | MFA 데이터 | TOE |
| V10 | `sso_users` (ALTER) | 사용자 출처 컬럼 추가 (source: LOCAL/LDAP) | TOE |

### 6.2 세션 저장소

- **구현**: `InMemorySessionStore` (ConcurrentHashMap 기반)
- **특성**: 서버 재시작 시 세션 소멸 (설계 의도 - 보안 강화)
- **인터페이스**: `SessionStore` (향후 Redis/DB 구현으로 교체 가능)

---

## 7. CC 모드가 아키텍처에 미치는 영향

### 7.1 구성요소 활성화 비교

| 구성요소 | 기본 모드 | CC 모드 | 분류 |
|----------|---------|---------|------|
| OIDC Core (6 엔드포인트) | 활성 | 활성 | TOE |
| Auth/MFA API | 활성 | 활성 | TOE |
| 감사 로그 조회 | 활성 | 활성 | TOE |
| Client/User/Role 관리 API | 활성 | **비활성** | @ExtendedFeature |
| Session 관리 API | 활성 | **비활성** | @ExtendedFeature |
| Client Credentials Grant | 활성 | **비활성** | @ExtendedFeature |
| Swagger UI / OpenAPI | 활성 | **비활성** | springdoc 비활성화 |
| Admin Console | 활성 | **비배포** | replicas: 0 |
| Actuator (info, metrics) | 활성 | **비활성** | health만 유지 |

### 7.2 하드닝 매개변수 변화

| 매개변수 | 기본값 | CC 모드 | 변경 방향 |
|---------|--------|---------|----------|
| 세션 타임아웃 | 60분 | 30분 | 단축 |
| 최대 동시 세션 | 5 | 3 | 축소 |
| 로그인 실패 임계 | 5회 | 3회 | 축소 |
| 계정 잠금 시간 | 30분 | 60분 | 연장 |
| 비밀번호 최소 길이 | 8자 | 12자 | 강화 |
| 비밀번호 이력 수 | 5개 | 10개 | 강화 |
| Rate Limit | 10/초 | 5/초 | 축소 |
| Refresh Token 유효기간 | 24시간 | 12시간 | 단축 |
| MFA 대기 시간 | 5분 | 3분 | 단축 |

---

## 8. 확장 레이어 아키텍처 (SPI)

### 8.1 Extension Layer 개요

AuthFusion은 SPI(Service Provider Interface) 패턴을 통해 향후 다양한 프로토콜과 인증 방식을 확장할 수 있다.

```
┌───────────────────────────────────────┐
│          Extension Layer (SPI)        │
│                                       │
│  ┌─────────┐  ┌──────┐  ┌──────────┐ │
│  │ FIDO2/  │  │ SAML │  │ SCIM 2.0 │ │
│  │ WebAuthn│  │ 2.0  │  │ (프로비전)│ │
│  └────┬────┘  └──┬───┘  └────┬─────┘ │
│       │          │           │        │
│  ┌────▼──────────▼───────────▼─────┐  │
│  │     SPI Interface Layer         │  │
│  │  AuthenticationProvider SPI     │  │
│  │  ProtocolAdapter SPI            │  │
│  │  UserStoreProvider SPI          │  │
│  └────────────────┬────────────────┘  │
└────────────────────┼───────────────────┘
                     │
          ┌──────────▼───────────┐
          │    SSO Server Core   │
          │    (TOE)             │
          └──────────────────────┘
```

### 8.2 확장 포인트

| SPI | 용도 | 현재 구현 | 향후 계획 |
|-----|------|----------|----------|
| AuthenticationProvider | 인증 방식 확장 | Password, TOTP, LDAP | FIDO2/WebAuthn |
| ProtocolAdapter | 프로토콜 확장 | OIDC | SAML 2.0 |
| UserStoreProvider | 사용자 저장소 확장 | PostgreSQL, LDAP | SCIM 2.0 |
| GrantHandler | OAuth2 그랜트 확장 | AuthCode, Refresh, ClientCred | Device Code |

---

## 9. 배포 토폴로지

### 9.1 표준 배포 (Docker Compose)

```
┌─────────────────────────────────────────────┐
│              Docker Host                    │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │ SSO      │  │ Admin    │  │ Vault    │  │
│  │ Server   │  │ Console  │  │ 1.17     │  │
│  │ :8080    │  │ :3000    │  │ :8200    │  │
│  └────┬─────┘  └────┬─────┘  └──────────┘  │
│       │              │                       │
│  ┌────▼──────────────▼──────────────────┐   │
│  │        authfusion-net (bridge)       │   │
│  └──────────────┬───────────────────────┘   │
│                  │                           │
│  ┌──────────────▼───────────────────────┐   │
│  │         PostgreSQL 16 :5432          │   │
│  └──────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

### 9.2 CC 모드 배포

```
┌─────────────────────────────────────────────┐
│              Docker Host                    │
│                                             │
│  ┌──────────┐               ┌──────────┐   │
│  │ SSO      │               │ Vault    │   │
│  │ Server   │               │ 1.17     │   │
│  │ :8080    │               │ :8200    │   │
│  │ (cc mode)│               │          │   │
│  └────┬─────┘               └──────────┘   │
│       │                                     │
│  ┌────▼────────────────────────────────┐    │
│  │       authfusion-net (bridge)       │    │
│  └──────────────┬──────────────────────┘    │
│                  │                           │
│  ┌──────────────▼──────────────────────┐    │
│  │        PostgreSQL 16 :5432          │    │
│  └─────────────────────────────────────┘    │
│                                             │
│  [Admin Console: 비배포 (replicas: 0)]      │
└─────────────────────────────────────────────┘
```

### 9.3 프로덕션 HA 배포

```
                     ┌────────────┐
                     │   Nginx    │
                     │ TLS 종료   │
                     │  :443      │
                     └──────┬─────┘
                            │
                  ┌─────────┼──────────┐
                  │                    │
           ┌──────▼──────┐    ┌───────▼─────┐
           │ SSO Server  │    │ SSO Server  │
           │ Instance 1  │    │ Instance 2  │
           │ :8080       │    │ :8080       │
           └──────┬──────┘    └───────┬─────┘
                  │                    │
           ┌──────▼────────────────────▼─────┐
           │       PostgreSQL 16              │
           │    (Primary + Standby)           │
           └─────────────────────────────────┘
```

---

## 10. 보안 아키텍처 요약

| 보안 영역 | 구현 기술 | 관련 SFR |
|-----------|----------|---------|
| 인증 프로토콜 | OIDC (Authorization Code + PKCE) | FIA_UAU.1, FIA_UID.1 |
| 비밀번호 보호 | BCrypt (cost=12) | FIA_SOS.1 |
| 다중 인증 | TOTP (RFC 6238, HMAC-SHA1, 6자리, 30초 주기) | FIA_UAU.1 |
| 토큰 서명 | RS256 (RSA 2048-bit + SHA-256), Nimbus JOSE+JWT | FCS_COP.1 |
| 키 보관 | AES-256-GCM (마스터 키 → SHA-256 → AES 키, 12-byte IV) | FCS_COP.1, FCS_CKM.1 |
| 키 분배 | JWKS 엔드포인트 (공개키만 노출) | FCS_CKM.2 |
| 접근 제어 | Spring Security + RBAC + URL 패턴 | FDP_ACC.1 |
| 무차별 대입 보호 | Caffeine Cache 기반 시도 횟수 추적 + 계정 잠금 | FIA_AFL.1 |
| 감사 | PostgreSQL sso_audit_events 테이블, 10개 이벤트 유형 | FAU_GEN.1, FAU_GEN.2 |
| 세션 보호 | 타임아웃 + 최대 동시 세션 제한 | FTA_SSL.3, FIA_USB.1 |
| 통신 보호 | Nginx TLS 1.2+ (운영환경 책임) | (운영환경) |

---

## 11. 관련 문서

- [Identity Engine 아키텍처](identity-engine.md) - 인증 엔진 상세
- [Token Service 아키텍처](token-service.md) - 토큰/키 관리 상세
- [감사 로깅 아키텍처](audit-logging.md) - 감사 서브시스템 상세
- [관리 평면 아키텍처](admin-plane.md) - 관리 콘솔/API 상세
- [TOE 경계 정의서](../cc/toe-boundary.md) - CC TOE 경계
- [평가 구성 정의서](../cc/evaluated-configuration.md) - CC 평가 구성

---

## 12. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
