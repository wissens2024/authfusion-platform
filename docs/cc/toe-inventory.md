# TOE 소스코드 인벤토리

## 1. 개요

본 문서는 AuthFusion Platform의 TOE(Target of Evaluation) 소스코드 인벤토리를 정의한다.
각 소스코드 파일의 분류(TOE/EXT/SHARED)와 관련 SFR(Security Functional Requirement)을 명시하여
CC(Common Criteria) 평가 범위를 명확히 한다.

### 분류 기준

| 분류 | 설명 |
|------|------|
| **TOE** | CC 평가 대상에 포함되는 보안 기능 구현 코드 |
| **EXT** | 평가 대상 외 확장 기능 (관리 편의 등) |
| **SHARED** | TOE와 EXT 모두에서 사용되는 공통 코드 |

---

## 2. SSO Server 소스코드 인벤토리

### 2.1 OIDC 엔드포인트 (oidc.endpoint)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `oidc.endpoint` | `AuthorizationEndpoint` | TOE | FIA_UAU.1 | OAuth2 인가 요청 처리 (`/oauth2/authorize`) |
| `oidc.endpoint` | `TokenEndpoint` | TOE | FIA_UAU.1 | 토큰 발급 요청 처리 (`/oauth2/token`) |
| `oidc.endpoint` | `UserInfoEndpoint` | TOE | FIA_UAU.1 | 사용자 정보 조회 (`/oauth2/userinfo`) |
| `oidc.endpoint` | `JwksEndpoint` | TOE | FCS_CKM.1 | JWKS 공개키 세트 제공 (`/.well-known/jwks.json`) |
| `oidc.endpoint` | `DiscoveryEndpoint` | TOE | FIA_UAU.1 | OIDC Discovery 메타데이터 (`/.well-known/openid-configuration`) |
| `oidc.endpoint` | `RevocationEndpoint` | TOE | FIA_UAU.1 | 토큰 폐기 처리 (`/oauth2/revoke`) |

### 2.2 OIDC 그랜트 핸들러 (oidc.grant)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `oidc.grant` | `AuthorizationCodeGrantHandler` | TOE | FIA_UAU.1 | Authorization Code + PKCE 그랜트 처리 |
| `oidc.grant` | `RefreshTokenGrantHandler` | TOE | FIA_UAU.1 | Refresh Token 그랜트 처리 |
| `oidc.grant` | `ClientCredentialsGrantHandler` | EXT | - | Client Credentials 그랜트 처리 (M2M 통신) |

### 2.3 OIDC 서비스 (oidc.service)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `oidc.service` | `AuthorizationService` | TOE | FIA_UAU.1 | 인가 요청 검증 및 코드 발급 로직 |
| `oidc.service` | `PkceValidator` | TOE | FIA_UAU.1 | PKCE(code_challenge/code_verifier) 검증 |
| `oidc.service` | `ScopeService` | TOE | FDP_ACC.1 | OAuth2 스코프 관리 및 검증 |
| `oidc.service` | `TokenService` | TOE | FCS_COP.1 | 토큰 생성/검증/폐기 통합 서비스 |

### 2.4 JWT 모듈 (jwt) - **필수 TOE 구성요소 (CC 평가 필수 포함)**

> **키 관리(RSA 키 생성/서명/로테이션/암호화 저장)는 TOE 필수 구성요소이며, Extension으로 분리할 수 없다.**

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `jwt` | `JwtTokenProvider` | **TOE [필수]** | FCS_COP.1 | JWT 토큰 생성 (RS256 서명) |
| `jwt` | `JwtTokenParser` | **TOE [필수]** | FCS_COP.1 | JWT 토큰 파싱 및 서명 검증 |
| `jwt` | `JwkProvider` | **TOE [필수]** | FCS_CKM.1 | JWK(JSON Web Key) 형식 공개키 제공 |
| `jwt` | `KeyPairManager` | **TOE [필수]** | FCS_CKM.1 | RSA 키 페어 생성/관리/로테이션 |
| `jwt` | `KeyEncryptionService` | **TOE [필수]** | FCS_COP.1 | 서명키 암호화 보관 (AES-256-GCM) |
| `jwt` | `TokenClaims` | **TOE [필수]** | - | JWT 클레임 데이터 모델 |
| `jwt` | `SigningKeyEntity` | **TOE [필수]** | - | 서명키 영속화 엔티티 |
| `jwt` | `SigningKeyRepository` | **TOE [필수]** | - | 서명키 JPA 리포지토리 |

### 2.5 사용자 인증 (user)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `user.controller` | `AuthController` | TOE | FIA_UAU.1 | 로그인/로그아웃 API (`/api/v1/auth/*`) |
| `user.controller` | `UserController` | EXT | - | 사용자 CRUD 관리 API (`/api/v1/users`) |
| `user.service` | `UserService` | TOE | FIA_UID.1 | 사용자 조회/생성/수정 비즈니스 로직 |
| `user.service` | `AuthService` | TOE | FIA_UAU.1 | 인증 처리 (자격증명 검증) |
| `user.service` | `PasswordHashService` | TOE | FCS_COP.1 | 비밀번호 해싱 (BCrypt/Argon2) |

### 2.6 MFA 모듈 (mfa) - **필수 TOE 구성요소 (CC 평가 필수 포함)**

> **MFA(TOTP)는 TOE 필수 구성요소이며, Extension으로 분리할 수 없다.**

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `mfa` | `TotpService` | **TOE [필수]** | FIA_UAU.5, FCS_COP.1 | TOTP 코드 생성/검증 (RFC 6238, HMAC-SHA1) |
| `mfa` | `MfaSessionService` | **TOE [필수]** | FIA_UAU.5 | MFA 대기 세션 관리 |
| `mfa` | `MfaController` | **TOE [필수]** | FIA_UAU.5 | MFA API 엔드포인트 (`/api/v1/mfa/*`) |
| `mfa` | `RecoveryCodeService` | **TOE [필수]** | FIA_UAU.5 | 복구 코드 생성/검증 (BCrypt 해싱) |
| `mfa` | `TotpSecretEntity` | **TOE [필수]** | - | TOTP 시크릿 영속화 엔티티 (AES-256-GCM 암호화) |
| `mfa` | `RecoveryCodeEntity` | **TOE [필수]** | - | 복구 코드 영속화 엔티티 |
| `mfa` | `MfaPendingSessionEntity` | **TOE [필수]** | - | MFA 대기 세션 엔티티 |

### 2.7 클라이언트 관리 (client)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `client.controller` | `ClientController` | EXT | - | OAuth2 클라이언트 CRUD API (`/api/v1/clients`) |

### 2.7 역할 기반 접근제어 (rbac)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `rbac.controller` | `RoleController` | EXT | - | 역할 CRUD 관리 API (`/api/v1/roles`) |

### 2.8 세션 관리 (session)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `session` | `SessionService` | TOE | FTA_SSL.3 | 세션 생성/검증/종료 서비스 |
| `session` | `SessionStore` | TOE | FTA_SSL.3 | 세션 저장소 인터페이스 |
| `session` | `InMemorySessionStore` | TOE | FTA_SSL.3 | 인메모리 세션 저장소 구현체 |
| `session` | `SsoSession` | TOE | - | SSO 세션 데이터 모델 |
| `session` | `SessionStatus` | TOE | - | 세션 상태 열거형 |
| `session` | `SessionInfo` | TOE | - | 세션 정보 DTO |
| `session.controller` | `SessionController` | EXT | - | 세션 조회/관리 API (`/api/v1/sessions`) |

### 2.9 보안 설정 (security)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `security` | `SecurityConfig` | TOE | FDP_ACC.1 | Spring Security 보안 정책 설정 |
| `security` | `BruteForceProtectionService` | TOE | FIA_AFL.1 | 무차별 대입 공격 방어 (계정 잠금) |
| `security` | `RateLimitFilter` | TOE | FIA_AFL.1 | API 요청 빈도 제한 필터 |
| `security` | `LoginAttemptEntity` | TOE | FIA_AFL.1 | 로그인 시도 기록 엔티티 |
| `security` | `CorsProperties` | TOE | FTP_TRP.1 | CORS 정책 설정 속성 |

### 2.10 감사 로그 (audit)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `audit` | `AuditService` | TOE | FAU_GEN.1 | 감사 이벤트 생성/저장 서비스 |
| `audit` | `AuditEventEntity` | TOE | FAU_GEN.1 | 감사 이벤트 영속화 엔티티 |
| `audit` | `AuditEventRepository` | TOE | FAU_GEN.1 | 감사 이벤트 JPA 리포지토리 |
| `audit` | `AuditController.getEvents` | TOE | FAU_SAR.1 | 감사 로그 조회 API (`/api/v1/audit/events`) |
| `audit.controller` | `AuditStatisticsController` | EXT | - | 감사 통계 API (대시보드용) |

### 2.11 웹 페이지 (web)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `web` | `LoginPageController` | TOE | FIA_UAU.1 | 로그인 페이지 렌더링 (Thymeleaf) |
| `web` | `ErrorPageController` | TOE | - | 에러 페이지 렌더링 |
| `web` | `ConsentPageController` | EXT | - | OAuth2 동의 페이지 렌더링 |

### 2.12 설정 (config)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `config` | `SecurityConfig` | TOE | FDP_ACC.1 | 보안 필터 체인 설정 |
| `config` | `GlobalExceptionHandler` | TOE | - | 전역 예외 처리 (보안 정보 노출 방지) |
| `config` | `JacksonConfig` | TOE | - | JSON 직렬화 설정 |
| `config` | `WebMvcConfig` | TOE | - | Spring MVC 설정 |
| `config` | `OpenApiConfig` | EXT | - | Swagger/OpenAPI 문서 설정 |

---

## 3. SSO Agent 소스코드 인벤토리

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `agent.filter` | `SsoAuthenticationFilter` | TOE | FIA_UAU.1 | 서블릿 필터 기반 SSO 인증 처리 |
| `agent.filter` | `TokenValidationFilter` | TOE | FCS_COP.1 | JWT 토큰 검증 필터 |
| `agent.config` | `SsoAgentAutoConfiguration` | TOE | - | Spring Boot Auto-Configuration |
| `agent.config` | `SsoAgentProperties` | TOE | - | SSO Agent 설정 속성 |
| `agent.client` | `SsoServerClient` | TOE | FTP_TRP.1 | SSO Server 통신 클라이언트 |
| `agent.token` | `TokenCache` | TOE | - | 토큰 캐시 관리 |
| `agent.token` | `TokenValidator` | TOE | FCS_COP.1 | JWT 토큰 검증 로직 |
| `agent.session` | `AgentSessionManager` | TOE | FTA_SSL.3 | Agent 측 세션 관리 |
| `agent.model` | `AuthenticatedUser` | TOE | - | 인증된 사용자 정보 모델 |

---

## 4. Admin Console 소스코드 인벤토리 (products/admin-console) - TOE

Admin Console은 관리 보안 기능(사용자 관리, 감사 로그 조회, 정책 관리)을 제공하므로 TOE에 포함한다.

| 디렉터리/파일 | 분류 | SFR | 설명 |
|---------------|------|-----|------|
| `src/app/login/` | **TOE** | FIA_UAU.1 | 관리자 로그인 페이지 |
| `src/app/dashboard/` | **TOE** | FMT_SMF.1 | 대시보드 (인증 통계, 시스템 상태) |
| `src/app/users/` | **TOE** | FMT_SMF.1 | 사용자 관리 페이지 |
| `src/app/clients/` | **TOE** | FMT_SMF.1 | 클라이언트 관리 페이지 |
| `src/app/roles/` | **TOE** | FDP_ACC.1 | 역할 관리 페이지 |
| `src/app/sessions/` | **TOE** | FTA_SSL.3 | 세션 모니터링/관리 페이지 |
| `src/app/audit/` | **TOE** | FAU_SAR.1 | 감사 로그 조회 페이지 |
| `src/app/settings/` | **TOE** | FMT_SMF.1 | 시스템 설정 페이지 |
| `src/lib/api.ts` | **TOE** | - | SSO Server API 클라이언트 |
| `src/components/` | **TOE** | - | 공통 UI 컴포넌트 (Sidebar, Header 등) |

---

## 5. 통계 요약

| 분류 | 파일 수 | 비율 |
|------|---------|------|
| TOE (SSO Server) | 48 + 7(MFA) | 약 72% |
| TOE (Admin Console) | 10 | 약 13% |
| TOE (SSO Agent) | 9 | 약 12% |
| EXT | 9 | 약 12% |
| SHARED | 3 | 약 4% |
| **합계** | **약 77** | **100%** |

> 주: MFA 7개 파일과 Admin Console 10개 파일이 TOE 필수로 추가됨.

---

## 6. SFR 매핑 요약

| SFR | 관련 TOE 파일 수 | 주요 기능 영역 |
|-----|------------------|----------------|
| FIA_UAU.1 | 13 | 사용자/클라이언트 인증 |
| FIA_UAU.5 | 7 | 복합 인증 메커니즘 (ID/PW + TOTP) [필수] |
| FIA_UID.1 | 1 | 사용자 식별 |
| FIA_AFL.1 | 3 | 인증 실패 대응 (계정 잠금) |
| FIA_USB.1 | 1 | 사용자-주체 바인딩 |
| FCS_CKM.1 | 3 | 암호키 생성 (RSA 키 페어) |
| FCS_CKM.2 | 1 | 암호키 분배 |
| FCS_CKM.4 | 1 | 암호키 파기 |
| FCS_COP.1 | 7 | 암호 연산 (서명/검증/해싱) |
| FDP_ACC.1 | 2 | 접근 제어 정책 |
| FDP_ACF.1 | 1 | 접근 제어 기능 |
| FAU_GEN.1 | 3 | 감사 데이터 생성 |
| FAU_GEN.2 | 1 | 사용자 신원 연관 |
| FAU_SAR.1 | 1 | 감사 검토 |
| FTA_SSL.3 | 4 | 세션 종료 |
| FTP_TRP.1 | 2 | 신뢰 경로/채널 |
| FMT_SMF.1 | 4 | 관리 기능 (Admin Console) |

---

## 7. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
| 1.1 | 2026-03-17 | AuthFusion Team | MFA 모듈 필수 TOE 추가, JWT 모듈 필수 표기, Admin Console 인벤토리 추가, FIA_UAU.5/FMT_SMF.1 추가 |
