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

### 2.4 JWT 모듈 (jwt)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `jwt` | `JwtTokenProvider` | TOE | FCS_COP.1 | JWT 토큰 생성 (RS256 서명) |
| `jwt` | `JwtTokenParser` | TOE | FCS_COP.1 | JWT 토큰 파싱 및 서명 검증 |
| `jwt` | `JwkProvider` | TOE | FCS_CKM.1 | JWK(JSON Web Key) 형식 공개키 제공 |
| `jwt` | `KeyPairManager` | TOE | FCS_CKM.1 | RSA 키 페어 생성/관리/로테이션 |
| `jwt` | `KeyEncryptionService` | TOE | FCS_COP.1 | 서명키 암호화 보관 (AES-256-GCM) |
| `jwt` | `TokenClaims` | TOE | - | JWT 클레임 데이터 모델 |
| `jwt` | `SigningKeyEntity` | TOE | - | 서명키 영속화 엔티티 |
| `jwt` | `SigningKeyRepository` | TOE | - | 서명키 JPA 리포지토리 |

### 2.5 사용자 인증 (user)

| 패키지 | 파일 | 분류 | SFR | 설명 |
|--------|------|------|-----|------|
| `user.controller` | `AuthController` | TOE | FIA_UAU.1 | 로그인/로그아웃 API (`/api/v1/auth/*`) |
| `user.controller` | `UserController` | EXT | - | 사용자 CRUD 관리 API (`/api/v1/users`) |
| `user.service` | `UserService` | TOE | FIA_UID.1 | 사용자 조회/생성/수정 비즈니스 로직 |
| `user.service` | `AuthService` | TOE | FIA_UAU.1 | 인증 처리 (자격증명 검증) |
| `user.service` | `PasswordHashService` | TOE | FCS_COP.1 | 비밀번호 해싱 (BCrypt/Argon2) |

### 2.6 클라이언트 관리 (client)

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

## 4. 통계 요약

| 분류 | 파일 수 | 비율 |
|------|---------|------|
| TOE | 48 | 약 80% |
| EXT | 9 | 약 15% |
| SHARED | 3 | 약 5% |
| **합계** | **60** | **100%** |

---

## 5. SFR 매핑 요약

| SFR | 관련 TOE 파일 수 | 주요 기능 영역 |
|-----|------------------|----------------|
| FIA_UAU.1 | 12 | 사용자/클라이언트 인증 |
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

---

## 6. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
