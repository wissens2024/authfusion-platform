# 관리 평면 아키텍처 (Admin Plane Architecture)

## AuthFusion SSO Platform v1.0

---

## 1. 개요

관리 평면(Admin Plane)은 SSO Server의 관리 REST API와 Admin Console(Next.js 웹 UI)로 구성된다.
관리자가 사용자, OAuth2 클라이언트, 역할, 세션, 감사 로그를 관리하고 시스템 상태를 모니터링할 수 있는 기능을 제공한다.

### 1.1 CC 분류

관리 평면의 대부분은 **Non-TOE**(@ExtendedFeature)로 분류된다.
CC 모드에서는 관리 API가 비활성화되고, Admin Console은 배포되지 않는다.
단, 감사 로그 조회 API(`/api/v1/audit/events`)는 FAU 요구사항 지원을 위해 TOE에 포함된다.

| 구성요소 | CC 분류 | CC 모드 동작 |
|----------|---------|------------|
| Admin Console (Next.js) | Non-TOE | 비배포 (replicas: 0) |
| 관리 API (Client/User/Role/Session) | @ExtendedFeature | 비활성화 (denyAll) |
| 감사 통계 API | @ExtendedFeature | 비활성화 |
| 감사 로그 조회 API | **TOE** | 활성 유지 |
| Swagger UI / OpenAPI | @ExtendedFeature | 비활성화 |

### 1.2 구성 다이어그램

```
┌──────────────────────────────────────────────────────────────────┐
│                    Admin Plane                                    │
│                                                                  │
│  ┌───────────────────────┐        ┌───────────────────────────┐ │
│  │  Admin Console         │  JWT   │  SSO Server REST API      │ │
│  │  (Next.js 14)          │───────>│                           │ │
│  │                        │        │  TOE:                     │ │
│  │  /login                │        │  ├─ GET /api/v1/audit/    │ │
│  │  /dashboard            │        │  │     events             │ │
│  │  /users                │        │  ├─ POST /api/v1/auth/*   │ │
│  │  /clients              │        │  └─ POST /api/v1/mfa/*    │ │
│  │  /roles                │        │                           │ │
│  │  /sessions             │        │  @ExtendedFeature:        │ │
│  │  /audit                │        │  ├─ /api/v1/users         │ │
│  │  /settings             │        │  ├─ /api/v1/clients       │ │
│  │                        │        │  ├─ /api/v1/roles         │ │
│  │  [Non-TOE]             │        │  ├─ /api/v1/sessions      │ │
│  │  [CC: 비배포]          │        │  └─ /api/v1/audit/stats   │ │
│  └───────────────────────┘        └───────────────┬───────────┘ │
│                                                    │              │
│                                          ┌─────────▼───────────┐ │
│                                          │   PostgreSQL 16     │ │
│                                          │   (운영환경)         │ │
│                                          └─────────────────────┘ │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Admin Console

### 2.1 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Next.js | 14 | React 프레임워크 (App Router) |
| React | 18 | UI 컴포넌트 라이브러리 |
| TypeScript | strict mode | 타입 안전성 |
| Tailwind CSS | - | 유틸리티 우선 CSS |
| Heroicons | - | 아이콘 |
| Fetch API | - | SSO Server API 통신 |

**위치**: `products/admin-console/`
**포트**: 3000 (개발 모드: `npm run dev`)
**경로 별칭**: `@/*` (TypeScript path alias)

### 2.2 페이지 구성

| 페이지 | 경로 | 기능 | 의존 API |
|--------|------|------|---------|
| 로그인 | `/login` | 관리자 인증 (MFA 지원) | `/api/v1/auth/login`, `/api/v1/auth/mfa/verify` |
| 대시보드 | `/` | 시스템 현황 요약 | `/api/v1/audit/statistics` |
| 사용자 목록 | `/users` | 사용자 CRUD, 소스 필터(LOCAL/LDAP) | `/api/v1/users` |
| 사용자 상세 | `/users/[id]` | 프로필, 역할, MFA, 세션 | `/api/v1/users/{id}`, `/api/v1/mfa/status` |
| 클라이언트 목록 | `/clients` | OAuth2 클라이언트 관리 | `/api/v1/clients` |
| 역할 관리 | `/roles` | RBAC 역할/권한 관리 | `/api/v1/roles` |
| 세션 관리 | `/sessions` | 활성 세션 조회/강제 종료 | `/api/v1/sessions` |
| 감사 로그 | `/audit` | 보안 이벤트 조회/필터 | `/api/v1/audit/events` |
| 설정 | `/settings` | 서버/MFA/LDAP/확장 설정 | (내부 설정 API) |

### 2.3 인증 방식

Admin Console은 SSO Server의 인증 API를 사용하여 관리자를 인증한다.

```
1. 관리자 → Admin Console /login 페이지 접근
2. username + password 입력
3. Admin Console → POST /api/v1/auth/login (SSO Server)
4. (MFA 활성 시) TOTP 코드 입력 → POST /api/v1/auth/mfa/verify
5. JWT 토큰 수신 → 클라이언트 측 저장
6. 이후 모든 API 호출에 Authorization: Bearer <JWT> 헤더 포함
7. 서버 측 렌더링(SSR) 시: SSO_SERVER_INTERNAL_URL 사용
```

**환경변수:**

| 변수 | 설명 | 예시 |
|------|------|------|
| `NEXT_PUBLIC_SSO_SERVER_URL` | 클라이언트 측 API URL | `http://localhost:8081` |
| `SSO_SERVER_INTERNAL_URL` | 서버 측(SSR) 내부 URL | `http://sso-server:8081` |

### 2.4 RBAC 기반 접근 제어

Admin Console 접근에는 `ADMIN` 역할이 필요하다.

```
관리 API 호출
    │
    ▼
JWT에서 roles 클레임 추출
    │
    ▼
roles.contains("ADMIN") ?
    │
    ├── YES → API 실행
    │
    └── NO → 403 Forbidden
```

---

## 3. 관리 REST API

### 3.1 TOE API (CC 모드에서도 활성)

| Method | 엔드포인트 | 설명 | 인증 |
|--------|-----------|------|------|
| POST | `/api/v1/auth/login` | 관리자 로그인 | 불필요 |
| POST | `/api/v1/auth/mfa/verify` | MFA 검증 | mfaToken |
| POST | `/api/v1/auth/logout` | 로그아웃 | JWT |
| GET | `/api/v1/audit/events` | 감사 이벤트 조회 | JWT (ADMIN) |
| POST | `/api/v1/mfa/setup` | TOTP 설정 | JWT |
| POST | `/api/v1/mfa/verify-setup` | TOTP 설정 검증 | JWT |
| POST | `/api/v1/mfa/verify` | TOTP 코드 검증 | JWT |
| GET | `/api/v1/mfa/status` | MFA 상태 조회 | JWT |

### 3.2 @ExtendedFeature API (CC 모드에서 비활성)

#### 사용자 관리 (`UserController`)

| Method | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/users` | 사용자 목록 (페이징, 검색) |
| GET | `/api/v1/users/{id}` | 사용자 상세 조회 |
| POST | `/api/v1/users` | 사용자 생성 |
| PUT | `/api/v1/users/{id}` | 사용자 정보 수정 |
| DELETE | `/api/v1/users/{id}` | 사용자 비활성화 |
| PUT | `/api/v1/users/{id}/password` | 비밀번호 변경 |

#### OAuth2 클라이언트 관리 (`ClientController`)

| Method | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/clients` | 클라이언트 목록 |
| GET | `/api/v1/clients/{id}` | 클라이언트 상세 |
| POST | `/api/v1/clients` | 클라이언트 등록 |
| PUT | `/api/v1/clients/{id}` | 클라이언트 수정 |
| DELETE | `/api/v1/clients/{id}` | 클라이언트 삭제 |

#### 역할 관리 (`RoleController`)

| Method | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/roles` | 역할 목록 |
| POST | `/api/v1/roles` | 역할 생성 |
| PUT | `/api/v1/roles/{id}` | 역할 수정 |
| DELETE | `/api/v1/roles/{id}` | 역할 삭제 |
| POST | `/api/v1/roles/{id}/users/{userId}` | 사용자에 역할 부여 |
| DELETE | `/api/v1/roles/{id}/users/{userId}` | 사용자에서 역할 해제 |

#### 세션 관리 (`SessionController`)

| Method | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/sessions` | 활성 세션 목록 |
| GET | `/api/v1/sessions/users/{userId}` | 사용자별 세션 |
| DELETE | `/api/v1/sessions/{sessionId}` | 세션 강제 종료 |
| DELETE | `/api/v1/sessions/users/{userId}` | 사용자 전체 세션 종료 |

#### 감사 통계 (`AuditStatisticsController`)

| Method | 엔드포인트 | 설명 |
|--------|-----------|------|
| GET | `/api/v1/audit/statistics` | 감사 이벤트 통계 |

#### Swagger UI (`OpenApiConfig`)

| 엔드포인트 | 설명 |
|-----------|------|
| `/swagger-ui.html` | Swagger UI 페이지 |
| `/swagger-ui/**` | Swagger UI 리소스 |
| `/api-docs/**` | OpenAPI 3.0 스펙 (JSON) |
| `/v3/api-docs/**` | OpenAPI 3.0 스펙 |

---

## 4. RBAC 모델

### 4.1 역할 기반 접근 제어 구조

```
┌────────────┐     ┌─────────────────┐     ┌─────────────┐
│  sso_users │     │ sso_user_roles  │     │  sso_roles  │
│            │────>│                 │<────│             │
│  id (PK)   │     │ user_id (FK)    │     │ id (PK)     │
│  username   │     │ role_id (FK)    │     │ name        │
│  email      │     │                 │     │ description │
└────────────┘     └─────────────────┘     └──────┬──────┘
                                                   │
                   ┌─────────────────┐             │
                   │ sso_client_roles│<────────────┘
                   │                 │
                   │ client_id (FK)  │
                   │ role_id (FK)    │
                   └─────────────────┘
```

### 4.2 기본 역할

| 역할 | 설명 | 권한 |
|------|------|------|
| `ADMIN` | 시스템 관리자 | 모든 관리 API 접근 |
| `USER` | 일반 사용자 | 자신의 프로필/MFA 관리만 |
| `AUDITOR` | 감사 관리자 | 감사 로그 조회 전용 |

### 4.3 역할 매핑

- **사용자-역할**: `sso_user_roles` 테이블 (다대다)
- **클라이언트-역할**: `sso_client_roles` 테이블 (다대다)
- **JWT 클레임**: 토큰 발급 시 사용자의 역할 목록을 `roles` 클레임에 포함

```json
{
  "roles": ["USER", "ADMIN"]
}
```

---

## 5. CC 모드 비활성화 메커니즘

### 5.1 @ExtendedFeature + @ConditionalOnExtendedMode

```java
@ExtendedFeature("클라이언트 관리 API")
@ConditionalOnExtendedMode
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    // authfusion.sso.cc.extended-features-enabled=false 시 빈 미등록
}
```

`@ConditionalOnExtendedMode`는 `authfusion.sso.cc.extended-features-enabled` 속성을 확인하여
`false`인 경우 해당 빈을 Spring 컨텍스트에 등록하지 않는다.

### 5.2 SecurityConfig denyAll()

빈 미등록과 별개로, `SecurityConfig`에서 CC 모드 시 관리 API 패턴을 `denyAll()`로 설정하여
이중 보호한다.

```java
if (!extendedEnabled) {
    auth.requestMatchers(
        "/api/v1/clients/**",
        "/api/v1/users/**",
        "/api/v1/roles/**",
        "/api/v1/sessions/**",
        "/api/v1/audit/statistics",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api-docs/**",
        "/v3/api-docs/**",
        "/consent"
    ).denyAll();
}
```

### 5.3 Docker Compose CC 오버라이드

```yaml
# docker-compose.cc.yml
services:
  admin-console:
    deploy:
      replicas: 0    # Admin Console 비배포
```

### 5.4 비활성화 계층 요약

```
1층: Spring Boot Auto-Configuration
    @ConditionalOnExtendedMode → Bean 미등록

2층: Spring Security
    SecurityConfig → denyAll() (HTTP 403)

3층: OpenAPI/Swagger
    springdoc.enabled=false → 문서 비노출

4층: Docker Compose
    replicas: 0 → Admin Console 비배포
```

---

## 6. API 규약

### 6.1 공통 규약

| 항목 | 규약 |
|------|------|
| 기본 경로 | `/api/v1/` |
| 인증 | `Authorization: Bearer <JWT>` |
| Content-Type | `application/json` |
| 날짜 형식 | ISO 8601 (`2026-03-04T10:30:00Z`) |
| 페이징 | `page` (0부터), `size` (기본 20) |
| null 처리 | Jackson `non_null` (null 필드 생략) |
| 에러 응답 | `{ "error": "...", "message": "...", "status": 400 }` |

### 6.2 HTTP 상태 코드

| 코드 | 의미 | 사용 예 |
|------|------|---------|
| 200 | 성공 | 조회, 수정 성공 |
| 201 | 생성 완료 | 사용자/클라이언트/역할 생성 |
| 204 | 내용 없음 | 삭제, 로그아웃 성공 |
| 400 | 잘못된 요청 | 유효성 검증 실패 |
| 401 | 인증 필요 | JWT 누락/만료 |
| 403 | 권한 없음 | 역할 부족, CC 모드 차단 |
| 404 | 리소스 없음 | 존재하지 않는 ID |
| 409 | 충돌 | 중복 username/email |
| 429 | 요청 과다 | Rate Limit 초과 |
| 500 | 서버 오류 | 내부 오류 |

---

## 7. 확장 관리

### 7.1 Platform 확장 모듈

Admin Console은 Platform 확장 모듈(keyhub, connect, automation)의 상태를 확인하고 관리할 수 있다.

| 확장 모듈 | 관리 기능 |
|----------|----------|
| `platform/keyhub` | SSH 키, API 키, 인증서 관리 |
| `platform/connect` | 외부 시스템 연동 상태 모니터링 |
| `platform/automation` | 워크플로 실행/예약/결과 조회 |

### 7.2 SPI 확장 등록

향후 SPI 기반 확장이 추가되면 Admin Console에서 등록/관리할 수 있다:

```
/settings/extensions
  ├── FIDO2/WebAuthn 확장
  ├── SAML 2.0 어댑터
  └── SCIM 2.0 프로비저닝
```

---

## 8. 운영 모니터링

### 8.1 Actuator 엔드포인트

| 엔드포인트 | 기본 모드 | CC 모드 | 설명 |
|-----------|---------|---------|------|
| `/actuator/health` | 활성 (상세) | 활성 (최소) | 서비스 상태 |
| `/actuator/info` | 활성 | **비활성** | 빌드 정보 |
| `/actuator/metrics` | 활성 | **비활성** | 성능 지표 |

### 8.2 Health Check

```json
// 기본 모드 (show-details: when_authorized)
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}

// CC 모드 (show-details: never)
{
  "status": "UP"
}
```

### 8.3 Admin Console 헬스체크

```
GET /api/health (Admin Console)
→ 200 OK
```

Docker Compose에서 Admin Console의 상태를 확인하는 데 사용된다.

---

## 9. 보안 고려사항

### 9.1 API 보안

- 모든 관리 API는 JWT 인증 필수 (ADMIN 역할)
- CORS 설정으로 허용 출처 제한 (`allowed-origins`)
- Rate Limit 적용 (기본 10/초, CC 모드 5/초)
- CSRF 비활성화 (Stateless API, JWT 기반)

### 9.2 Admin Console 보안

- 서버 사이드 렌더링(SSR) 시 내부 URL 사용 (`SSO_SERVER_INTERNAL_URL`)
- 클라이언트 측 JWT 저장 (HttpOnly Cookie 권장)
- XSS 방지 (React의 자동 이스케이프)
- CSP(Content Security Policy) 헤더 설정 권장

### 9.3 감사

- 모든 관리 API 호출은 감사 로그에 기록
- 사용자 생성/수정/삭제: `USER_MANAGEMENT` 이벤트
- 클라이언트 등록/수정/삭제: `CLIENT_MANAGEMENT` 이벤트
- 역할 변경: `ROLE_MANAGEMENT` 이벤트
- 세션 강제 종료: `SESSION_MANAGEMENT` 이벤트

---

## 10. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
