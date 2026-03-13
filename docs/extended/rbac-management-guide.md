# RBAC 관리 가이드

## 역할 기반 접근 제어 (RBAC) 관리 지침서

---

## 1. 개요

본 문서는 AuthFusion SSO Server의 역할 기반 접근 제어(RBAC) 시스템을 관리하는 방법을 안내한다.
RBAC는 사용자에게 역할을 할당하고, 역할에 권한을 부여하여 접근을 제어하는 방식이다.

### 1.1 RBAC 모델 구조

```
사용자 (User) ──── N:M ────── 역할 (Role)
                                  │
                               1:N
                                  │
                              권한 (Permission)
```

---

## 2. 기본 역할

### 2.1 시스템 기본 역할

AuthFusion SSO Server는 다음 기본 역할을 제공한다:

| 역할명 | 설명 | 기본 권한 | 삭제 가능 |
|--------|------|----------|:---------:|
| `ADMIN` | 시스템 관리자 | 모든 권한 | X |
| `USER` | 일반 사용자 | 자기 정보 조회/수정 | X |

### 2.2 기본 권한 목록

| 권한 ID | 설명 | ADMIN | USER |
|---------|------|:-----:|:----:|
| `READ_USERS` | 사용자 목록 조회 | O | X |
| `WRITE_USERS` | 사용자 생성/수정/삭제 | O | X |
| `READ_SELF` | 본인 정보 조회 | O | O |
| `WRITE_SELF` | 본인 정보 수정 | O | O |
| `READ_CLIENTS` | 클라이언트 목록 조회 | O | X |
| `WRITE_CLIENTS` | 클라이언트 생성/수정/삭제 | O | X |
| `READ_ROLES` | 역할 목록 조회 | O | X |
| `WRITE_ROLES` | 역할 생성/수정/삭제 | O | X |
| `ASSIGN_ROLES` | 사용자에 역할 할당/해제 | O | X |
| `READ_SESSIONS` | 세션 목록 조회 | O | X |
| `MANAGE_SESSIONS` | 세션 강제 종료 | O | X |
| `READ_AUDIT` | 감사 로그 조회 | O | X |
| `READ_STATISTICS` | 통계 조회 | O | X |
| `MANAGE_KEYS` | 서명키 로테이션 | O | X |
| `ALL` | 모든 권한 (관리자용) | O | X |

---

## 3. 역할 관리

### 3.1 역할 생성

#### Admin Console

1. **역할 관리** 메뉴 접근
2. **새 역할 생성** 버튼 클릭
3. 역할명, 설명, 권한 설정
4. **저장** 클릭

#### API

```bash
curl -X POST \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "OPERATOR",
    "description": "운영자 - 조회 및 세션 관리 권한",
    "permissions": [
      "READ_USERS",
      "READ_CLIENTS",
      "READ_SESSIONS",
      "MANAGE_SESSIONS",
      "READ_AUDIT",
      "READ_STATISTICS"
    ]
  }' \
  "https://sso.aines.kr/api/v1/roles"
```

### 3.2 역할 수정

```bash
curl -X PUT \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "운영자 - 조회/세션/키 관리 권한",
    "permissions": [
      "READ_USERS",
      "READ_CLIENTS",
      "READ_SESSIONS",
      "MANAGE_SESSIONS",
      "READ_AUDIT",
      "READ_STATISTICS",
      "MANAGE_KEYS"
    ]
  }' \
  "https://sso.aines.kr/api/v1/roles/{roleId}"
```

### 3.3 역할 삭제

```bash
curl -X DELETE \
  -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/roles/{roleId}"
```

> **주의**: 사용자에게 할당된 역할은 삭제할 수 없다. 먼저 모든 사용자에서 해당 역할을 해제해야 한다.

### 3.4 역할 목록 조회

```bash
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/roles"
```

---

## 4. 사용자-역할 할당

### 4.1 역할 할당

```bash
# 사용자에게 역할 할당
curl -X POST \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "roleIds": ["role-uuid-operator"]
  }' \
  "https://sso.aines.kr/api/v1/users/{userId}/roles"
```

### 4.2 역할 해제

```bash
# 사용자에서 역할 해제
curl -X DELETE \
  -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/users/{userId}/roles/{roleId}"
```

### 4.3 할당 규칙

- 사용자는 복수의 역할을 가질 수 있다
- 역할의 권한은 합집합(Union)으로 적용된다
- 최소 1개의 역할이 할당되어야 한다 (`USER` 역할 기본)
- `ADMIN` 역할 할당/해제는 다른 `ADMIN` 사용자만 가능

### 4.4 역할 변경 시 즉시 적용

역할 변경 시 다음 토큰 갱신(Refresh) 시점부터 새 역할이 적용된다.
즉시 적용이 필요한 경우 해당 사용자의 활성 세션을 강제 종료한다:

```bash
# 사용자의 모든 세션 종료 (역할 즉시 적용)
curl -X DELETE \
  -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/sessions?username={username}"
```

---

## 5. 클라이언트-역할 연동

### 5.1 클라이언트별 역할 범위

클라이언트가 요청할 수 있는 역할 정보는 스코프에 의해 제어된다:

| 스코프 | 토큰에 포함되는 정보 |
|--------|---------------------|
| `openid` | `sub` (사용자 ID) |
| `roles` | `roles` (역할 목록) |
| `profile` | `name`, `preferred_username` |

### 5.2 JWT 토큰 내 역할 클레임

`roles` 스코프가 허용된 클라이언트로 인증 시 JWT에 역할 정보가 포함된다:

```json
{
  "sub": "user-uuid",
  "iss": "https://sso.aines.kr",
  "aud": "client-id",
  "exp": 1709470200,
  "iat": 1709469300,
  "roles": ["USER", "OPERATOR"],
  "scope": "openid profile roles"
}
```

### 5.3 SSO Agent에서의 역할 기반 접근 제어

SSO Agent가 설치된 레거시 애플리케이션에서 역할 기반 접근 제어를 적용하는 예시:

```java
// SSO Agent 필터에서 추출된 사용자 정보 활용
AuthenticatedUser user = SsoAuthenticationFilter.getCurrentUser(request);

if (user.hasRole("ADMIN")) {
    // 관리자 기능 접근 허용
} else if (user.hasRole("OPERATOR")) {
    // 운영자 기능 접근 허용
} else {
    // 일반 사용자 기능만 허용
}
```

---

## 6. 역할 설계 권장 사항

### 6.1 역할 설계 원칙

1. **최소 권한 원칙**: 업무에 필요한 최소한의 권한만 부여
2. **직무 분리**: 상충되는 권한을 분리하여 별도 역할로 관리
3. **역할 계층 최소화**: 불필요한 중첩을 피하고 명확한 역할 구분
4. **정기 검토**: 분기별 역할 할당 현황 검토 및 불필요한 권한 회수

### 6.2 권장 역할 구성

| 역할 | 대상 | 권한 |
|------|------|------|
| ADMIN | 시스템 관리자 | 모든 권한 |
| SECURITY_ADMIN | 보안 관리자 | 감사 로그, 세션 관리, 키 관리 |
| OPERATOR | 운영자 | 조회 전체, 세션 관리 |
| HELPDESK | 헬프데스크 | 사용자 조회, 비밀번호 초기화, 잠금 해제 |
| USER | 일반 사용자 | 본인 정보 조회/수정 |

### 6.3 역할 구성 예시

```bash
# 보안 관리자 역할 생성
curl -X POST \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "SECURITY_ADMIN",
    "description": "보안 관리자 - 감사/세션/키 관리",
    "permissions": [
      "READ_AUDIT",
      "READ_SESSIONS",
      "MANAGE_SESSIONS",
      "MANAGE_KEYS",
      "READ_STATISTICS"
    ]
  }' \
  "https://sso.aines.kr/api/v1/roles"

# 헬프데스크 역할 생성
curl -X POST \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "HELPDESK",
    "description": "헬프데스크 - 사용자 지원",
    "permissions": [
      "READ_USERS",
      "READ_SELF",
      "WRITE_SELF"
    ]
  }' \
  "https://sso.aines.kr/api/v1/roles"
```

---

## 7. 감사 추적

역할 관련 모든 변경은 감사 로그에 기록된다:

| 이벤트 유형 | 설명 |
|------------|------|
| `ROLE_CREATED` | 새 역할 생성 |
| `ROLE_MODIFIED` | 역할 권한 변경 |
| `ROLE_DELETED` | 역할 삭제 |
| `ROLE_ASSIGNED` | 사용자에 역할 할당 |
| `ROLE_REVOKED` | 사용자에서 역할 해제 |

감사 로그 조회:

```bash
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/audit/events?eventType=ROLE_ASSIGNED"
```

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
