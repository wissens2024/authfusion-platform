# 관리 API 명세

## AuthFusion SSO Server 관리 API Reference

---

## 1. 개요

본 문서는 AuthFusion SSO Server의 관리(EXT) API를 명세한다.
이 API들은 CC 평가 대상(TOE) 외부의 확장 기능으로, 관리 편의성을 위해 제공된다.

### 1.1 공통 사항

- **Base URL**: `https://{host}:8080/api/v1`
- **인증**: `Authorization: Bearer <access_token>` (JWT)
- **Content-Type**: `application/json`
- **필요 역할**: `ADMIN` 역할 필수 (별도 명시 시 예외)

### 1.2 공통 응답 형식

#### 성공 응답

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-03-03T10:30:00Z"
}
```

#### 오류 응답

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "오류 설명"
  },
  "timestamp": "2026-03-03T10:30:00Z"
}
```

### 1.3 페이징 응답 형식

```json
{
  "content": [ ... ],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## 2. 클라이언트 관리 API

### 2.1 클라이언트 등록

- **URL**: `POST /api/v1/clients`
- **역할**: ADMIN

#### 요청

```json
{
  "clientName": "Sample Application",
  "clientType": "CONFIDENTIAL",
  "redirectUris": [
    "https://app.example.com/callback",
    "https://app.example.com/silent-renew"
  ],
  "allowedScopes": ["openid", "profile", "email"],
  "grantTypes": ["authorization_code", "refresh_token"],
  "tokenEndpointAuthMethod": "client_secret_basic",
  "accessTokenTtl": 900,
  "refreshTokenTtl": 3600
}
```

#### 응답 (201 Created)

```json
{
  "id": "client-uuid",
  "clientId": "generated-client-id",
  "clientSecret": "generated-client-secret",
  "clientName": "Sample Application",
  "clientType": "CONFIDENTIAL",
  "redirectUris": ["https://app.example.com/callback"],
  "allowedScopes": ["openid", "profile", "email"],
  "grantTypes": ["authorization_code", "refresh_token"],
  "createdAt": "2026-03-03T10:30:00Z"
}
```

### 2.2 클라이언트 목록 조회

- **URL**: `GET /api/v1/clients`
- **역할**: ADMIN

#### 쿼리 매개변수

| 매개변수 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 |
| `search` | String | - | 이름 검색 |
| `clientType` | String | - | 클라이언트 유형 필터 |

### 2.3 클라이언트 상세 조회

- **URL**: `GET /api/v1/clients/{clientId}`
- **역할**: ADMIN

### 2.4 클라이언트 수정

- **URL**: `PUT /api/v1/clients/{clientId}`
- **역할**: ADMIN

#### 요청

```json
{
  "clientName": "Updated Application Name",
  "redirectUris": ["https://new-app.example.com/callback"],
  "allowedScopes": ["openid", "profile"],
  "accessTokenTtl": 600
}
```

### 2.5 클라이언트 삭제

- **URL**: `DELETE /api/v1/clients/{clientId}`
- **역할**: ADMIN

### 2.6 클라이언트 시크릿 재생성

- **URL**: `POST /api/v1/clients/{clientId}/regenerate-secret`
- **역할**: ADMIN

---

## 3. 사용자 관리 API

### 3.1 사용자 생성

- **URL**: `POST /api/v1/users`
- **역할**: ADMIN

#### 요청

```json
{
  "username": "newuser",
  "password": "SecureP@ssw0rd!",
  "email": "newuser@example.com",
  "name": "홍길동",
  "roles": ["USER"],
  "enabled": true,
  "forcePasswordChange": true
}
```

#### 응답 (201 Created)

```json
{
  "id": "user-uuid",
  "username": "newuser",
  "email": "newuser@example.com",
  "name": "홍길동",
  "roles": ["USER"],
  "enabled": true,
  "createdAt": "2026-03-03T10:30:00Z"
}
```

### 3.2 사용자 목록 조회

- **URL**: `GET /api/v1/users`
- **역할**: ADMIN

#### 쿼리 매개변수

| 매개변수 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 |
| `search` | String | - | 이름/이메일 검색 |
| `enabled` | Boolean | - | 활성화 상태 필터 |
| `role` | String | - | 역할 필터 |

### 3.3 사용자 상세 조회

- **URL**: `GET /api/v1/users/{userId}`
- **역할**: ADMIN

### 3.4 사용자 수정

- **URL**: `PUT /api/v1/users/{userId}`
- **역할**: ADMIN

### 3.5 사용자 비활성화

- **URL**: `PATCH /api/v1/users/{userId}/disable`
- **역할**: ADMIN

### 3.6 사용자 비밀번호 초기화

- **URL**: `POST /api/v1/users/{userId}/reset-password`
- **역할**: ADMIN

#### 요청

```json
{
  "newPassword": "NewSecureP@ss1!",
  "forcePasswordChange": true
}
```

### 3.7 사용자 잠금 해제

- **URL**: `POST /api/v1/users/{userId}/unlock`
- **역할**: ADMIN

---

## 4. 역할(RBAC) 관리 API

### 4.1 역할 생성

- **URL**: `POST /api/v1/roles`
- **역할**: ADMIN

#### 요청

```json
{
  "name": "OPERATOR",
  "description": "운영자 역할",
  "permissions": ["READ_USERS", "READ_SESSIONS", "READ_AUDIT"]
}
```

### 4.2 역할 목록 조회

- **URL**: `GET /api/v1/roles`
- **역할**: ADMIN

#### 응답 (200 OK)

```json
[
  {
    "id": "role-uuid-1",
    "name": "ADMIN",
    "description": "관리자",
    "permissions": ["ALL"],
    "userCount": 2,
    "createdAt": "2026-03-01T00:00:00Z"
  },
  {
    "id": "role-uuid-2",
    "name": "USER",
    "description": "일반 사용자",
    "permissions": ["READ_SELF"],
    "userCount": 150,
    "createdAt": "2026-03-01T00:00:00Z"
  }
]
```

### 4.3 역할 수정

- **URL**: `PUT /api/v1/roles/{roleId}`
- **역할**: ADMIN

### 4.4 역할 삭제

- **URL**: `DELETE /api/v1/roles/{roleId}`
- **역할**: ADMIN

> **주의**: 사용자에게 할당된 역할은 삭제할 수 없다.

### 4.5 사용자에 역할 할당

- **URL**: `POST /api/v1/users/{userId}/roles`
- **역할**: ADMIN

#### 요청

```json
{
  "roleIds": ["role-uuid-1", "role-uuid-2"]
}
```

### 4.6 사용자 역할 해제

- **URL**: `DELETE /api/v1/users/{userId}/roles/{roleId}`
- **역할**: ADMIN

---

## 5. 세션 관리 API

### 5.1 활성 세션 목록 조회

- **URL**: `GET /api/v1/sessions`
- **역할**: ADMIN

#### 쿼리 매개변수

| 매개변수 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 |
| `status` | String | ACTIVE | 세션 상태 (ACTIVE/EXPIRED/TERMINATED) |
| `username` | String | - | 사용자 필터 |

#### 응답 (200 OK)

```json
{
  "content": [
    {
      "sessionId": "session-uuid",
      "username": "user01",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "status": "ACTIVE",
      "createdAt": "2026-03-03T09:00:00Z",
      "lastAccessedAt": "2026-03-03T10:25:00Z",
      "expiresAt": "2026-03-03T10:55:00Z"
    }
  ],
  "totalElements": 45
}
```

### 5.2 세션 강제 종료

- **URL**: `DELETE /api/v1/sessions/{sessionId}`
- **역할**: ADMIN

### 5.3 사용자 전체 세션 종료

- **URL**: `DELETE /api/v1/sessions?username={username}`
- **역할**: ADMIN

---

## 6. 감사 통계 API

### 6.1 감사 이벤트 통계 조회

- **URL**: `GET /api/v1/audit/statistics`
- **역할**: ADMIN

#### 쿼리 매개변수

| 매개변수 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `from` | DateTime | 24시간 전 | 시작 일시 |
| `to` | DateTime | 현재 | 종료 일시 |
| `interval` | String | HOUR | 집계 간격 (HOUR/DAY/WEEK) |

#### 응답 (200 OK)

```json
{
  "summary": {
    "totalEvents": 1234,
    "loginSuccess": 800,
    "loginFailure": 45,
    "tokenIssued": 950,
    "tokenRevoked": 12,
    "accountLocked": 3
  },
  "timeline": [
    {
      "timestamp": "2026-03-03T09:00:00Z",
      "loginSuccess": 50,
      "loginFailure": 3,
      "tokenIssued": 60
    }
  ]
}
```

### 6.2 이벤트 유형별 통계

- **URL**: `GET /api/v1/audit/statistics/by-type`
- **역할**: ADMIN

### 6.3 사용자별 활동 통계

- **URL**: `GET /api/v1/audit/statistics/by-user`
- **역할**: ADMIN

### 6.4 클라이언트별 토큰 발급 통계

- **URL**: `GET /api/v1/audit/statistics/by-client`
- **역할**: ADMIN

---

## 7. 오류 코드 목록

| HTTP 상태 | 오류 코드 | 설명 |
|-----------|----------|------|
| 400 | `INVALID_REQUEST` | 잘못된 요청 형식 |
| 400 | `VALIDATION_ERROR` | 입력값 검증 실패 |
| 401 | `UNAUTHORIZED` | 인증되지 않음 |
| 401 | `TOKEN_EXPIRED` | 토큰 만료 |
| 403 | `FORBIDDEN` | 권한 부족 |
| 404 | `NOT_FOUND` | 리소스를 찾을 수 없음 |
| 409 | `CONFLICT` | 중복 리소스 (이미 존재) |
| 422 | `PASSWORD_POLICY_VIOLATION` | 비밀번호 정책 위반 |
| 429 | `RATE_LIMIT_EXCEEDED` | 요청 빈도 제한 초과 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
