# TOE 경계 정의서 (TOE Boundary Definition)

## AuthFusion SSO Platform v1.0

---

## 1. 문서 개요

### 1.1 목적

본 문서는 AuthFusion SSO Platform의 TOE(Target of Evaluation) 경계를 명확히 정의한다.
CC(Common Criteria) 평가에서 TOE 경계는 평가 대상 범위를 결정하는 가장 핵심적인 문서이며,
어떤 구성요소가 평가 대상에 포함되고 어떤 구성요소가 운영환경에 속하는지를 명시한다.

### 1.2 적용 범위

- **평가 대상**: AuthFusion SSO Server v1.0, AuthFusion SSO Agent v1.0
- **CC 버전**: Common Criteria v3.1 Revision 5
- **보증 등급**: EAL2+ (증강: ALC_FLR.1)

### 1.3 용어 정의

| 용어 | 정의 |
|------|------|
| **TOE** | Target of Evaluation - CC 평가의 대상이 되는 IT 제품 또는 시스템 |
| **TSF** | TOE Security Functionality - TOE가 제공하는 보안 기능의 총체 |
| **운영환경** | TOE가 올바르게 동작하기 위해 의존하는 외부 구성요소 |
| **SFR** | Security Functional Requirement - 보안기능요구사항 |
| **CC 모드** | 확장 기능을 비활성화한 최소 TOE 구성 |

---

## 2. TOE 경계 다이어그램

### 2.1 물리적 경계 (Physical Boundary)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         AuthFusion Platform (배포물)                         │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      TOE 경계 (TOE Boundary)                          │  │
│  │                                                                       │  │
│  │  ┌─────────────────────────────┐  ┌─────────────────────────────┐    │  │
│  │  │     products/sso-server     │  │     products/sso-agent      │    │  │
│  │  │     (SSO Server - TOE)      │  │     (SSO Agent - TOE)       │    │  │
│  │  │                             │  │                             │    │  │
│  │  │  ┌───────────────────────┐  │  │  ┌───────────────────────┐  │    │  │
│  │  │  │ OIDC Provider Core    │  │  │  │ Authentication Filter │  │    │  │
│  │  │  │ - Authorization EP    │  │  │  │ - SsoAuthenticationF. │  │    │  │
│  │  │  │ - Token EP            │  │  │  │ - SsoAuthorizationF.  │  │    │  │
│  │  │  │ - UserInfo EP         │  │  │  │ - SsoLogoutFilter     │  │    │  │
│  │  │  │ - Revocation EP       │  │  │  └───────────────────────┘  │    │  │
│  │  │  │ - Discovery EP        │  │  │  ┌───────────────────────┐  │    │  │
│  │  │  │ - JWKS EP             │  │  │  │ Token Validation      │  │    │  │
│  │  │  └───────────────────────┘  │  │  │ - JwtTokenValidator   │  │    │  │
│  │  │  ┌───────────────────────┐  │  │  │ - JwksKeyResolver     │  │    │  │
│  │  │  │ Identity Engine       │  │  │  │ - TokenCache          │  │    │  │
│  │  │  │ - AuthController      │  │  │  └───────────────────────┘  │    │  │
│  │  │  │ - UserService         │  │  │  ┌───────────────────────┐  │    │  │
│  │  │  │ - PasswordHashService │  │  │  │ Access Control        │  │    │  │
│  │  │  │ - BruteForceProtect.  │  │  │  │ - AccessControlMgr    │  │    │  │
│  │  │  │ - LdapAuthProvider    │  │  │  │ - RoleBasedAccessChk  │  │    │  │
│  │  │  └───────────────────────┘  │  │  │ - UrlPatternMatcher   │  │    │  │
│  │  │  ┌───────────────────────┐  │  │  └───────────────────────┘  │    │  │
│  │  │  │ MFA Engine            │  │  │  ┌───────────────────────┐  │    │  │
│  │  │  │ - TotpService (6238)  │  │  │  │ Session Sync          │  │    │  │
│  │  │  │ - MfaSessionService   │  │  │  │ - AgentSessionManager │  │    │  │
│  │  │  │ - RecoveryCodeSvc     │  │  │  │ - SessionSyncService  │  │    │  │
│  │  │  └───────────────────────┘  │  │  └───────────────────────┘  │    │  │
│  │  │  ┌───────────────────────┐  │  │                             │    │  │
│  │  │  │ Token Service         │  │  └─────────────────────────────┘    │  │
│  │  │  │ - JwtTokenProvider    │  │                                      │  │
│  │  │  │ - JwtTokenParser      │  │                                      │  │
│  │  │  │ - KeyPairManager      │  │                                      │  │
│  │  │  │ - KeyEncryptionSvc    │  │                                      │  │
│  │  │  │ - JwkProvider         │  │                                      │  │
│  │  │  └───────────────────────┘  │                                      │  │
│  │  │  ┌───────────────────────┐  │                                      │  │
│  │  │  │ Audit Subsystem       │  │                                      │  │
│  │  │  │ - AuditService        │  │                                      │  │
│  │  │  │ - AuditEventEntity    │  │                                      │  │
│  │  │  │ - AuditEventRepo      │  │                                      │  │
│  │  │  └───────────────────────┘  │                                      │  │
│  │  │  ┌───────────────────────┐  │                                      │  │
│  │  │  │ Session Management    │  │                                      │  │
│  │  │  │ - SessionService      │  │                                      │  │
│  │  │  │ - SessionStore        │  │                                      │  │
│  │  │  └───────────────────────┘  │                                      │  │
│  │  │  ┌───────────────────────┐  │                                      │  │
│  │  │  │ Security Config       │  │                                      │  │
│  │  │  │ - SecurityConfig      │  │                                      │  │
│  │  │  │ - RateLimitFilter     │  │                                      │  │
│  │  │  └───────────────────────┘  │                                      │  │
│  │  │                             │                                      │  │
│  │  └─────────────────────────────┘                                      │  │
│  │                                                                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                     Non-TOE 구성요소 (배포물 내)                       │  │
│  │                                                                       │  │
│  │  ┌───────────────────┐  ┌───────────────────┐  ┌──────────────────┐  │  │
│  │  │ products/          │  │ platform/          │  │ platform/        │  │  │
│  │  │ admin-console     │  │ keyhub             │  │ connect          │  │  │
│  │  │ (관리 콘솔 UI)    │  │ (키 관리 허브)     │  │ (연동 커넥터)    │  │  │
│  │  └───────────────────┘  └───────────────────┘  └──────────────────┘  │  │
│  │  ┌───────────────────┐  ┌──────────────────────────────────────────┐  │  │
│  │  │ platform/          │  │ @ExtendedFeature 확장 컨트롤러          │  │  │
│  │  │ automation         │  │ - ClientController, RoleController      │  │  │
│  │  │ (자동화 워크플로)  │  │ - SessionController, ConsentPageCtrl   │  │  │
│  │  └───────────────────┘  │ - AuditStatisticsCtrl, OpenApiConfig    │  │  │
│  │                          │ - ClientCredentialsGrantHandler         │  │  │
│  │                          └──────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                        운영환경 (Operational Environment)                     │
│                                                                             │
│  ┌────────────────┐ ┌──────────────┐ ┌────────────┐ ┌───────────────────┐  │
│  │ PostgreSQL 16  │ │ HashiCorp    │ │ AD / LDAP  │ │ NTP Server        │  │
│  │ (DBMS)         │ │ Vault 1.17   │ │ Directory  │ │ (시간 동기화)     │  │
│  └────────────────┘ └──────────────┘ └────────────┘ └───────────────────┘  │
│  ┌────────────────┐ ┌──────────────┐ ┌────────────┐ ┌───────────────────┐  │
│  │ Thales Luna    │ │ SIEM         │ │ Nginx      │ │ OS / JVM          │  │
│  │ HSM (PKCS#11)  │ │ (로그 분석)  │ │ (Reverse   │ │ (실행 환경)       │  │
│  │                │ │              │ │  Proxy)    │ │                   │  │
│  └────────────────┘ └──────────────┘ └────────────┘ └───────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 논리적 경계 (Logical Boundary)

```
                    ┌──────────────────────────────┐
                    │       사용자 / 클라이언트      │
                    └──────────┬───────────────────┘
                               │ HTTPS (TLS 1.2+)
                    ┌──────────▼───────────────────┐
                    │      Nginx (Reverse Proxy)    │  ← 운영환경
                    └──────────┬───────────────────┘
                               │
          ┌────────────────────┼──────────────────────┐
          │                    │                       │
┌─────────▼─────────┐ ┌───────▼──────────┐  ┌────────▼─────────┐
│   OIDC Endpoints   │ │  Auth/MFA API    │  │  Admin API       │
│   (TOE)            │ │  (TOE)           │  │  (Non-TOE)       │
│                    │ │                  │  │  @ExtendedFeature │
│  /oauth2/authorize │ │ /api/v1/auth/*   │  │  /api/v1/users   │
│  /oauth2/token     │ │ /api/v1/mfa/*    │  │  /api/v1/clients │
│  /oauth2/userinfo  │ │                  │  │  /api/v1/roles   │
│  /oauth2/revoke    │ │                  │  │  /api/v1/sessions│
│  /.well-known/*    │ │                  │  │                  │
└─────────┬─────────┘ └───────┬──────────┘  └────────┬─────────┘
          │                    │                       │
          └────────────────────┼───────────────────────┘
                               │
                    ┌──────────▼───────────────────┐
                    │     Core Security Services    │  ← TOE
                    │                              │
                    │  TokenService, SessionService │
                    │  AuditService, KeyPairManager │
                    │  BruteForceProtectionService  │
                    │  TotpService, PasswordHashSvc │
                    └──────────┬───────────────────┘
                               │
              ┌────────────────┼──────────────────┐
              │                │                   │
   ┌──────────▼──────┐ ┌──────▼───────┐ ┌────────▼─────────┐
   │  PostgreSQL 16  │ │  Vault/HSM   │ │  LDAP/AD         │
   │  (운영환경)      │ │  (운영환경)  │ │  (운영환경)       │
   └─────────────────┘ └──────────────┘ └──────────────────┘
```

---

## 3. TOE 구성요소 상세

### 3.1 SSO Server (products/sso-server) - TOE

SSO Server는 TOE의 핵심 구성요소로, OIDC Provider 기능을 수행한다.

#### 3.1.1 디렉터리 수준 경계

| 디렉터리/패키지 | 분류 | 설명 |
|----------------|------|------|
| `com.authfusion.sso.oidc.endpoint` | **TOE** | OIDC 표준 엔드포인트 (6개) |
| `com.authfusion.sso.oidc.service` | **TOE** | OIDC 핵심 비즈니스 로직 |
| `com.authfusion.sso.oidc.grant` | **TOE/EXT** | 그랜트 핸들러 (AuthCode=TOE, ClientCred=EXT) |
| `com.authfusion.sso.oidc.model` | **TOE** | OIDC 데이터 모델 |
| `com.authfusion.sso.jwt` | **TOE** | JWT 토큰 생성/검증, RSA 키 관리 |
| `com.authfusion.sso.session` | **TOE** | SSO 세션 관리 |
| `com.authfusion.sso.audit` | **TOE** | 감사 로그 생성/저장/조회 |
| `com.authfusion.sso.security` | **TOE** | 보안 필터, 무차별 대입 보호, Rate Limit |
| `com.authfusion.sso.user.service` | **TOE** | 사용자 인증, 비밀번호 해싱/정책 |
| `com.authfusion.sso.user.controller.AuthController` | **TOE** | 인증 API 컨트롤러 |
| `com.authfusion.sso.mfa` | **TOE** | TOTP MFA, 복구 코드 |
| `com.authfusion.sso.config.SecurityConfig` | **TOE** | Spring Security 필터 체인 |
| `com.authfusion.sso.cc` | **TOE** | CC 어노테이션 (@ToeScope, @ExtendedFeature) |
| `com.authfusion.sso.client.controller.ClientController` | **EXT** | 클라이언트 관리 API |
| `com.authfusion.sso.user.controller.UserController` | **EXT** | 사용자 관리 API |
| `com.authfusion.sso.rbac.controller.RoleController` | **EXT** | 역할 관리 API |
| `com.authfusion.sso.session.controller.SessionController` | **EXT** | 세션 조회/관리 API |
| `com.authfusion.sso.audit.controller.AuditStatisticsController` | **EXT** | 감사 통계 API |
| `com.authfusion.sso.web.ConsentPageController` | **EXT** | 동의 페이지 컨트롤러 |
| `com.authfusion.sso.config.OpenApiConfig` | **EXT** | Swagger/OpenAPI 구성 |
| `db/migration/V1~V10` | **TOE** | Flyway DB 마이그레이션 스크립트 |
| `application.yml` | **TOE** | 기본 설정 |
| `application-cc.yml` | **TOE** | CC 하드닝 프로파일 설정 |

#### 3.1.2 TOE 보안 기능 매핑

| 보안 기능 | 관련 SFR | 구현 클래스 |
|-----------|---------|-------------|
| OIDC 인증 (Authorization Code + PKCE) | FIA_UAU.1, FIA_UID.1 | AuthorizationEndpoint, TokenEndpoint, PkceValidator |
| 비밀번호 인증 | FIA_UAU.1, FIA_SOS.1 | AuthController, UserService, PasswordHashService |
| TOTP 다중 인증 (RFC 6238) | FIA_UAU.1, FCS_COP.1 | TotpService, MfaSessionService, MfaController |
| 무차별 대입 보호 | FIA_AFL.1 | BruteForceProtectionService, RateLimitFilter |
| 보안 속성 바인딩 | FIA_USB.1 | SessionService, TokenService |
| JWT 서명/검증 (RS256) | FCS_COP.1 | JwtTokenProvider, JwtTokenParser |
| RSA 키 생성 | FCS_CKM.1 | KeyPairManager |
| 키 분배 (JWKS) | FCS_CKM.2 | JwkProvider, JwksEndpoint |
| 키 파기 | FCS_CKM.4 | KeyPairManager (rotateKeyPair) |
| 키 암호화 보관 (AES-256-GCM) | FCS_COP.1 | KeyEncryptionService |
| 감사 로그 생성 | FAU_GEN.1 | AuditService |
| 감사 주체 식별 | FAU_GEN.2 | AuditService (userId, ipAddress 기록) |
| 접근 제어 | FDP_ACC.1 | SecurityConfig, ScopeService |
| 세션 타임아웃 | FTA_SSL.3 | SessionService |

### 3.2 SSO Agent (products/sso-agent) - TOE

SSO Agent는 레거시 웹 애플리케이션에 SSO 기능을 부여하는 Servlet Filter 기반 JAR 라이브러리이다.

#### 3.2.1 디렉터리 수준 경계

| 디렉터리/패키지 | 분류 | 설명 |
|----------------|------|------|
| `com.authfusion.agent.filter` | **TOE** | Servlet Filter (인증, 인가, 로그아웃) |
| `com.authfusion.agent.token` | **TOE** | JWT 토큰 검증, JWKS 키 리졸버 |
| `com.authfusion.agent.session` | **TOE** | 에이전트 세션 관리, 서버 연동 |
| `com.authfusion.agent.access` | **TOE** | URL 패턴 기반 접근 제어, RBAC |
| `com.authfusion.agent.context` | **TOE** | 보안 컨텍스트 관리 |
| `com.authfusion.agent.config` | **TOE** | 에이전트 설정, 자동 구성 |
| `com.authfusion.agent.util` | **TOE** | 쿠키/리다이렉트 유틸리티 |
| `com.authfusion.agent.cc` | **TOE** | CC 어노테이션 |
| `com.authfusion.agent.annotation` | **TOE** | @EnableSsoAgent 어노테이션 |

#### 3.2.2 Agent TOE 보안 기능

| 보안 기능 | 관련 SFR | 구현 클래스 |
|-----------|---------|-------------|
| 요청 인증 필터링 | FIA_UAU.1 | SsoAuthenticationFilter |
| 토큰 서명 검증 (RS256) | FCS_COP.1 | JwtTokenValidator, JwksKeyResolver |
| URL 기반 접근 제어 | FDP_ACC.1 | AccessControlManager, RoleBasedAccessChecker |
| 세션 서버 동기화 | FIA_USB.1 | AgentSessionManager, SessionSyncService |
| SSO 로그아웃 | FTA_SSL.3 | SsoLogoutFilter |

---

## 4. Non-TOE 구성요소

### 4.1 배포물 내 Non-TOE

CC 모드에서 비활성화되거나 평가 범위에 포함되지 않는 구성요소이다.

#### 4.1.1 Admin Console (products/admin-console)

| 항목 | 내용 |
|------|------|
| **분류** | Non-TOE (관리 편의 UI) |
| **기술 스택** | Next.js 14, React 18, TypeScript |
| **역할** | SSO Server API를 통한 관리 기능 제공 |
| **CC 모드 동작** | `docker-compose.cc.yml`에서 `replicas: 0`으로 비배포 |
| **비-TOE 사유** | 관리 UI는 보안 기능을 직접 구현하지 않으며, SSO Server API를 경유하여 동작 |

#### 4.1.2 Platform 확장 (platform/*)

| 디렉터리 | 분류 | 설명 |
|----------|------|------|
| `platform/keyhub` | Non-TOE | 키 관리 허브 (커넥터, 정책, UI) |
| `platform/connect` | Non-TOE | 외부 시스템 연동 커넥터 (cloud, db, firewall, linux, vpn, waf) |
| `platform/automation` | Non-TOE | 자동화 워크플로 및 보고서 템플릿 |

#### 4.1.3 @ExtendedFeature 컨트롤러

CC 모드에서 `authfusion.sso.cc.extended-features-enabled=false` 설정 시 비활성화되는 컨트롤러들이다.

| 클래스 | 엔드포인트 | 비활성화 방식 |
|--------|-----------|--------------|
| `ClientController` | `/api/v1/clients/**` | @ConditionalOnExtendedMode + SecurityConfig denyAll |
| `UserController` | `/api/v1/users/**` | @ConditionalOnExtendedMode + SecurityConfig denyAll |
| `RoleController` | `/api/v1/roles/**` | @ConditionalOnExtendedMode + SecurityConfig denyAll |
| `SessionController` | `/api/v1/sessions/**` | @ConditionalOnExtendedMode + SecurityConfig denyAll |
| `AuditStatisticsController` | `/api/v1/audit/statistics` | @ConditionalOnExtendedMode + SecurityConfig denyAll |
| `ConsentPageController` | `/consent` | @ConditionalOnExtendedMode + SecurityConfig denyAll |
| `ClientCredentialsGrantHandler` | (내부 그랜트) | @ConditionalOnExtendedMode |
| `OpenApiConfig` | `/swagger-ui/**`, `/api-docs/**` | springdoc.enabled=false |

### 4.2 운영환경 (Operational Environment)

TOE의 올바른 동작을 위해 필요하지만 평가 대상에 포함되지 않는 외부 구성요소이다.

#### 4.2.1 DBMS - PostgreSQL 16

| 항목 | 내용 |
|------|------|
| **역할** | TOE 데이터 영속화 (사용자, 클라이언트, 세션, 감사 로그, 서명키) |
| **TOE 의존성** | JDBC를 통한 데이터 CRUD |
| **운영 요구사항** | TLS 암호화 연결, 접근 제어(pg_hba.conf), 정기 백업 |
| **비-TOE 사유** | 범용 DBMS이며, TOE 자체가 아닌 데이터 저장소 역할만 수행 |

#### 4.2.2 HashiCorp Vault 1.17

| 항목 | 내용 |
|------|------|
| **역할** | 시크릿 관리 (마스터 키, DB 비밀번호, LDAP 바인드 비밀번호) |
| **TOE 의존성** | 환경변수를 통한 시크릿 주입 |
| **운영 요구사항** | Vault 서버 고가용성, 접근 정책 설정, 감사 로깅 활성화 |
| **비-TOE 사유** | 외부 시크릿 관리 도구로, TOE에 환경변수를 제공하는 역할 |

#### 4.2.3 Thales Luna HSM (PKCS#11)

| 항목 | 내용 |
|------|------|
| **역할** | 하드웨어 기반 키 저장 및 암호 연산 (선택 사항) |
| **TOE 의존성** | KeyEncryptionService의 대체 구현으로 사용 가능 |
| **운영 요구사항** | PKCS#11 드라이버 설치, 파티션 및 접근 정책 설정 |
| **비-TOE 사유** | 외부 암호 모듈이며, TOE는 HSM 유무와 관계없이 소프트웨어 키 보호 제공 |

#### 4.2.4 LDAP / Active Directory

| 항목 | 내용 |
|------|------|
| **역할** | 외부 사용자 저장소 (선택 사항) |
| **TOE 의존성** | LDAP search-then-bind 인증 프로토콜 |
| **운영 요구사항** | LDAPS(TLS) 연결, 서비스 계정 바인드 DN 설정 |
| **비-TOE 사유** | 외부 디렉터리 서비스이며, TOE는 LDAP 프로토콜을 통해 인증 위임 |

#### 4.2.5 NTP Server

| 항목 | 내용 |
|------|------|
| **역할** | 시스템 시간 동기화 (감사 로그 타임스탬프, 토큰 유효성 검증) |
| **운영 요구사항** | Stratum 2 이하 NTP 서버, chrony/ntpd 구성 |
| **비-TOE 사유** | OS 수준 시간 동기화 서비스 |

#### 4.2.6 SIEM / 로그 수집기

| 항목 | 내용 |
|------|------|
| **역할** | 감사 로그 장기 보관 및 분석 |
| **운영 요구사항** | Syslog 또는 파일 기반 로그 전달, 보존 정책 설정 |
| **비-TOE 사유** | TOE가 생성한 감사 로그의 외부 보관/분석 도구 |

#### 4.2.7 OS / JVM 실행 환경

| 항목 | 내용 |
|------|------|
| **역할** | TOE 실행 기반 플랫폼 |
| **운영 요구사항** | Java 17+, Linux (RHEL 8+ / Ubuntu 22.04+), TLS 1.2+ 지원 |
| **비-TOE 사유** | 범용 실행 환경 |

#### 4.2.8 Nginx (Reverse Proxy)

| 항목 | 내용 |
|------|------|
| **역할** | TLS 종료, 로드 밸런싱, HTTP 헤더 관리 |
| **운영 요구사항** | TLS 1.2+ 인증서 구성, 보안 헤더 설정 |
| **비-TOE 사유** | 네트워크 인프라 구성요소 |

---

## 5. @ToeScope / @ExtendedFeature 어노테이션 체계

### 5.1 @ToeScope 어노테이션

TOE에 포함되는 클래스 또는 메소드에 부착한다. 리플렉션 스캔으로 TOE 인벤토리를 자동 생성할 수 있다.

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToeScope {
    String value() default "";          // 구성요소 설명
    String[] sfr() default {};          // 관련 SFR 식별자
}
```

- SSO Server: 약 54개 파일에 부착
- SSO Agent: 약 22개 파일에 부착

### 5.2 @ExtendedFeature 어노테이션

CC 최소 TOE에 포함되지 않는 확장 기능 클래스에 부착한다.

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtendedFeature {
    String value() default "";          // 확장 기능 설명
}
```

- SSO Server: 약 8개 파일에 부착

### 5.3 @ConditionalOnExtendedMode

`authfusion.sso.cc.extended-features-enabled` 설정에 따라 빈 등록을 조건부로 수행한다.
CC 모드(`false`)에서는 @ExtendedFeature가 붙은 빈이 Spring 컨텍스트에 등록되지 않는다.

---

## 6. CC 모드 경계 적용 메커니즘

### 6.1 설정 기반 분리

| 설정 키 | 기본값 | CC 모드 값 | 영향 |
|---------|--------|-----------|------|
| `authfusion.sso.cc.extended-features-enabled` | `true` | `false` | 확장 컨트롤러 비활성화 |
| `authfusion.sso.cc.mode` | - | `minimum` | 최소 TOE 모드 표시 |
| `springdoc.api-docs.enabled` | `true` | `false` | OpenAPI 문서 비활성화 |
| `springdoc.swagger-ui.enabled` | `true` | `false` | Swagger UI 비활성화 |

### 6.2 SecurityConfig 기반 엔드포인트 차단

CC 모드에서 `extendedEnabled=false`일 때 SecurityConfig의 `authorizeHttpRequests`에서 다음 패턴을 `denyAll()`로 설정한다:

- `/api/v1/clients/**`
- `/api/v1/users/**`
- `/api/v1/roles/**`
- `/api/v1/sessions/**`
- `/api/v1/audit/statistics`
- `/swagger-ui/**`, `/swagger-ui.html`
- `/api-docs/**`, `/v3/api-docs/**`
- `/consent`

### 6.3 Docker Compose CC 오버라이드

`docker-compose.cc.yml`은 `docker-compose.yml`을 오버라이드하여 다음을 적용한다:

- `SPRING_PROFILES_ACTIVE: cc,docker` (CC 프로파일 활성화)
- `AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED: "false"` (확장 기능 비활성화)
- `admin-console` 서비스 `replicas: 0` (관리 콘솔 비배포)

---

## 7. SFR 매핑 요약

### 7.1 TOE SFR 전체 목록

| SFR | 명칭 | TOE 구현 위치 |
|-----|------|--------------|
| FIA_UAU.1 | 인증 타이밍 | AuthorizationEndpoint, TokenEndpoint, AuthController, TotpService |
| FIA_UID.1 | 식별 타이밍 | AuthorizationEndpoint, AuthController |
| FIA_AFL.1 | 인증 실패 처리 | BruteForceProtectionService, RateLimitFilter |
| FIA_SOS.1 | 비밀 검증 | PasswordPolicyService, PasswordHashService |
| FIA_USB.1 | 사용자-주체 바인딩 | SessionService, TokenService |
| FCS_COP.1 | 암호 연산 | JwtTokenProvider (RS256), KeyEncryptionService (AES-256-GCM), TotpService (HMAC-SHA1) |
| FCS_CKM.1 | 암호키 생성 | KeyPairManager (RSA 2048-bit) |
| FCS_CKM.2 | 암호키 분배 | JwkProvider, JwksEndpoint (JWKS 공개키) |
| FCS_CKM.4 | 암호키 파기 | KeyPairManager (rotateKeyPair, 이전 키 비활성화) |
| FAU_GEN.1 | 감사 데이터 생성 | AuditService |
| FAU_GEN.2 | 사용자 식별 연관 | AuditService (userId, ipAddress, clientId 기록) |
| FDP_ACC.1 | 접근 제어 정책 | SecurityConfig, ScopeService, AccessControlManager(Agent) |
| FTA_SSL.3 | TSF 개시 세션 종료 | SessionService (timeout, maxSessions) |

---

## 8. 경계 검증 도구

### 8.1 TOE 인벤토리 자동 생성

`tools/cc/toe-diff` 도구를 사용하여 @ToeScope 어노테이션 기반 인벤토리를 자동 생성하고,
변경 사항을 추적할 수 있다.

### 8.2 경계 무결성 검증

CC 빌드 시(`mvn clean package -Pcc`) 다음이 자동 수행된다:

1. @ToeScope 인벤토리 스캔 및 보고서 생성
2. @ExtendedFeature 비활성화 검증
3. SBOM(CycloneDX) 생성
4. GPG 서명

---

## 9. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
