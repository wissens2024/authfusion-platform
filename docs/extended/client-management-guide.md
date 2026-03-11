# 클라이언트 관리 가이드

## OAuth2 클라이언트 관리 지침서

---

## 1. 개요

본 문서는 AuthFusion SSO Server에 OAuth2 클라이언트를 등록하고 관리하는 방법을 안내한다.
클라이언트는 OIDC 프로토콜을 통해 사용자 인증을 위임하는 애플리케이션을 의미한다.

### 1.1 대상 독자

- 애플리케이션 개발자
- SSO 관리자

---

## 2. 클라이언트 유형

### 2.1 유형 분류

| 유형 | 설명 | 시크릿 | 용도 |
|------|------|--------|------|
| **CONFIDENTIAL** | 서버 측 애플리케이션 | 필수 (안전 보관) | 웹 서버, 백엔드 서비스 |
| **PUBLIC** | 클라이언트 측 애플리케이션 | 없음 | SPA, 모바일 앱, 데스크톱 앱 |

### 2.2 유형별 지원 그랜트

| 그랜트 타입 | CONFIDENTIAL | PUBLIC |
|------------|:----------:|:-----:|
| authorization_code (+ PKCE) | O | O |
| refresh_token | O | O |
| client_credentials | O | X |

### 2.3 유형 선택 기준

```
클라이언트가 시크릿을 안전하게 보관할 수 있는가?
  │
  ├── 예 (서버 측 애플리케이션)
  │     └── CONFIDENTIAL
  │
  └── 아니오 (브라우저/모바일)
        └── PUBLIC (PKCE 필수)
```

---

## 3. 클라이언트 등록

### 3.1 Admin Console을 통한 등록

1. Admin Console(`https://sso.example.com:3000`)에 관리자로 로그인
2. 좌측 메뉴에서 **클라이언트 관리** 클릭
3. **새 클라이언트 등록** 버튼 클릭
4. 필수 항목 입력:
   - **클라이언트 이름**: 식별 가능한 이름
   - **클라이언트 유형**: CONFIDENTIAL 또는 PUBLIC
   - **리다이렉트 URI**: 인가 코드 수신 URI
   - **허용 스코프**: openid, profile, email 등
   - **그랜트 타입**: authorization_code, refresh_token 등
5. **등록** 버튼 클릭
6. 생성된 Client ID와 Client Secret 확인 및 안전 보관

### 3.2 API를 통한 등록

```bash
curl -X POST \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "내부 관리 시스템",
    "clientType": "CONFIDENTIAL",
    "redirectUris": [
      "https://internal.example.com/oauth/callback"
    ],
    "allowedScopes": ["openid", "profile", "email"],
    "grantTypes": ["authorization_code", "refresh_token"],
    "tokenEndpointAuthMethod": "client_secret_basic",
    "accessTokenTtl": 900,
    "refreshTokenTtl": 3600
  }' \
  "https://sso.example.com/api/v1/clients"
```

### 3.3 등록 시 주의사항

- **리다이렉트 URI**: HTTPS 필수 (CC 모드), localhost는 개발 환경에서만 허용
- **스코프**: 필요한 최소한의 스코프만 허용 (최소 권한 원칙)
- **Client Secret**: 등록 시 1회만 표시됨, 분실 시 재생성 필요
- **토큰 TTL**: CC 모드 기본값을 초과하는 값은 설정 불가

---

## 4. 클라이언트 설정 상세

### 4.1 리다이렉트 URI 설정

#### 규칙

- HTTPS 프로토콜 필수 (CC 모드)
- 와일드카드(`*`) 사용 불가
- 프래그먼트(`#`) 포함 불가
- 정확한 URI 매칭 (쿼리 스트링 제외)

#### 예시

```
허용:
  https://app.example.com/callback
  https://app.example.com/auth/redirect
  http://localhost:3000/callback  (개발 환경만)

불허:
  https://*.example.com/callback  (와일드카드)
  http://app.example.com/callback (HTTP, CC 모드)
  https://app.example.com/callback#fragment (프래그먼트)
```

### 4.2 스코프 설정

| 스코프 | 설명 | 포함 클레임 |
|--------|------|------------|
| `openid` | OIDC 인증 (필수) | `sub` |
| `profile` | 사용자 프로필 | `name`, `preferred_username` |
| `email` | 이메일 정보 | `email`, `email_verified` |
| `roles` | 역할 정보 | `roles` |
| `offline_access` | Refresh Token 발급 | - |

### 4.3 토큰 설정

| 설정 | 기본값 | 최소 | 최대 (CC 모드) | 설명 |
|------|--------|------|---------------|------|
| Access Token TTL | 900초 | 300초 | 3600초 | Access Token 유효 시간 |
| Refresh Token TTL | 3600초 | 600초 | 86400초 | Refresh Token 유효 시간 |
| ID Token TTL | 900초 | 300초 | 3600초 | ID Token 유효 시간 |

### 4.4 인증 방식 설정

| 방식 | 설명 | 유형 |
|------|------|------|
| `client_secret_basic` | HTTP Basic Auth 헤더 | CONFIDENTIAL |
| `client_secret_post` | POST 본문에 포함 | CONFIDENTIAL |
| `none` | 인증 없음 (PKCE 필수) | PUBLIC |

---

## 5. 클라이언트 수정

### 5.1 수정 가능 항목

| 항목 | 수정 가능 | 비고 |
|------|:---------:|------|
| 클라이언트 이름 | O | |
| 클라이언트 ID | X | 불변 |
| 클라이언트 유형 | X | 불변 (재등록 필요) |
| 리다이렉트 URI | O | |
| 허용 스코프 | O | |
| 그랜트 타입 | O | |
| 토큰 TTL | O | CC 모드 제한 적용 |
| Client Secret | 재생성만 | 이전 시크릿 즉시 무효화 |

### 5.2 수정 API 예시

```bash
curl -X PUT \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "내부 관리 시스템 v2",
    "redirectUris": [
      "https://internal-v2.example.com/oauth/callback"
    ],
    "allowedScopes": ["openid", "profile"]
  }' \
  "https://sso.example.com/api/v1/clients/{clientId}"
```

---

## 6. 클라이언트 삭제

### 6.1 삭제 전 확인 사항

- 해당 클라이언트로 발급된 모든 토큰이 폐기됨
- 활성 세션이 있는 경우 경고 표시
- 삭제 후 복구 불가

### 6.2 삭제 절차

```bash
# 클라이언트 삭제
curl -X DELETE \
  -H "Authorization: Bearer <admin-token>" \
  "https://sso.example.com/api/v1/clients/{clientId}"
```

### 6.3 감사 기록

클라이언트 생성/수정/삭제 시 감사 로그에 자동 기록된다:
- `CLIENT_CREATED`: 클라이언트 등록
- `CLIENT_MODIFIED`: 클라이언트 수정
- `CLIENT_DELETED`: 클라이언트 삭제

---

## 7. 클라이언트 시크릿 관리

### 7.1 시크릿 재생성

보안 사고 또는 정기 교체 시 시크릿을 재생성한다:

```bash
curl -X POST \
  -H "Authorization: Bearer <admin-token>" \
  "https://sso.example.com/api/v1/clients/{clientId}/regenerate-secret"
```

> **주의**: 이전 시크릿은 즉시 무효화된다. 해당 클라이언트를 사용하는 모든 애플리케이션의 설정을 업데이트해야 한다.

### 7.2 시크릿 보관 권장 사항

- 소스코드에 직접 기입하지 않는다
- 환경 변수 또는 시크릿 관리 도구(Vault 등) 사용
- 정기적 교체 (권장: 90일)
- 접근 권한을 최소 인원으로 제한

---

## 8. 클라이언트 연동 예시

### 8.1 Spring Boot 애플리케이션 (SSO Agent 사용)

```yaml
# application.yml
authfusion:
  agent:
    server-url: https://sso.example.com
    client-id: your-client-id
    client-secret: ${CLIENT_SECRET}
    redirect-uri: https://your-app.com/callback
    scopes: openid,profile,email
```

### 8.2 SPA (Public 클라이언트)

```javascript
// OIDC 설정 예시
const config = {
  authority: 'https://sso.example.com',
  client_id: 'spa-client-id',
  redirect_uri: 'https://spa.example.com/callback',
  response_type: 'code',
  scope: 'openid profile email',
  code_challenge_method: 'S256'
};
```

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
