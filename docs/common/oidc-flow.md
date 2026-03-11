# OIDC 프로토콜 흐름

## OpenID Connect Authorization Code + PKCE 흐름 상세

---

## 1. 개요

AuthFusion SSO Server는 OpenID Connect 1.0 프로토콜의 **Authorization Code + PKCE** 흐름을 핵심 인증 메커니즘으로 사용한다.
본 문서는 프로토콜 흐름, 토큰 생명주기, 각 단계의 보안 메커니즘을 상세히 기술한다.

### 1.1 지원 그랜트 타입

| 그랜트 타입 | 용도 | TOE 분류 |
|------------|------|----------|
| Authorization Code + PKCE | 사용자 인증 (기본) | TOE |
| Refresh Token | 토큰 갱신 | TOE |
| Client Credentials | 서비스 간 통신 (M2M) | EXT |

### 1.2 PKCE 필수 적용

AuthFusion은 모든 Authorization Code 흐름에 PKCE를 필수로 적용한다.
이는 Authorization Code 가로채기 공격을 방지하기 위한 보안 강화 조치이다.

- **지원 방식**: `S256` (SHA-256) 만 허용
- **`plain` 방식**: CC 모드에서 비허용

---

## 2. Authorization Code + PKCE 흐름

### 2.1 전체 흐름 다이어그램

```
  사용자                    클라이언트 앱              SSO Server
  (브라우저)                (RP)                      (OP)
     │                        │                         │
     │  1. 로그인 클릭        │                         │
     │───────────────────────>│                         │
     │                        │                         │
     │                        │  2. PKCE 생성           │
     │                        │  code_verifier 생성     │
     │                        │  code_challenge 계산    │
     │                        │                         │
     │  3. 인가 요청 리다이렉트│                         │
     │<───────────────────────│                         │
     │                        │                         │
     │  4. /oauth2/authorize 요청                       │
     │─────────────────────────────────────────────────>│
     │                                                  │
     │                                5. 요청 검증      │
     │                                - client_id 확인  │
     │                                - redirect_uri 확인│
     │                                - scope 확인      │
     │                                - PKCE 확인       │
     │                                                  │
     │  6. 로그인 페이지 표시                            │
     │<─────────────────────────────────────────────────│
     │                                                  │
     │  7. 자격증명 입력 (사용자명/비밀번호)              │
     │─────────────────────────────────────────────────>│
     │                                                  │
     │                                8. 자격증명 검증   │
     │                                - 계정 잠금 확인   │
     │                                - 비밀번호 검증    │
     │                                - 세션 생성        │
     │                                - 감사 로그 기록   │
     │                                                  │
     │  9. 인가 코드 발급 + 리다이렉트                   │
     │<─────────────────────────────────────────────────│
     │  302 → redirect_uri?code={code}&state={state}    │
     │                        │                         │
     │  10. 인가 코드 전달    │                         │
     │───────────────────────>│                         │
     │                        │                         │
     │                        │  11. 토큰 교환 요청     │
     │                        │  POST /oauth2/token     │
     │                        │  + code                 │
     │                        │  + code_verifier        │
     │                        │──────────────────────>│
     │                        │                         │
     │                        │        12. PKCE 검증    │
     │                        │        SHA256(verifier)  │
     │                        │         == challenge     │
     │                        │                         │
     │                        │        13. 토큰 발급    │
     │                        │        - Access Token   │
     │                        │        - ID Token       │
     │                        │        - Refresh Token  │
     │                        │                         │
     │                        │  14. 토큰 응답          │
     │                        │<──────────────────────│
     │                        │                         │
     │  15. 인증 완료         │                         │
     │<───────────────────────│                         │
     │                        │                         │
```

### 2.2 단계별 상세 설명

#### 단계 2: PKCE 코드 생성

클라이언트는 인가 요청 전에 PKCE 관련 값을 생성한다:

```
code_verifier  = 43~128자의 랜덤 문자열 (URL-safe Base64)
code_challenge = BASE64URL(SHA256(code_verifier))
```

```javascript
// 예시 (JavaScript)
function generateCodeVerifier() {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return base64UrlEncode(array);
}

function generateCodeChallenge(verifier) {
  const hash = await crypto.subtle.digest('SHA-256',
    new TextEncoder().encode(verifier));
  return base64UrlEncode(new Uint8Array(hash));
}
```

#### 단계 4: 인가 요청

```
GET /oauth2/authorize?
  response_type=code
  &client_id=my-client
  &redirect_uri=https://app.example.com/callback
  &scope=openid profile email
  &state=random-state-value
  &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM
  &code_challenge_method=S256
  &nonce=random-nonce-value
```

#### 단계 9: 인가 코드 발급

```
HTTP/1.1 302 Found
Location: https://app.example.com/callback?
  code=SplxlOBeZQQYbYS6WxSbIA
  &state=random-state-value
```

인가 코드 특성:
- 1회용 (사용 후 즉시 무효화)
- 유효 시간: 5분 (CC 모드)
- 특정 client_id에 바인딩
- code_challenge에 바인딩

#### 단계 11: 토큰 교환 요청

```
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=https://app.example.com/callback
&client_id=my-client
&client_secret=client-secret-value
&code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

#### 단계 12: PKCE 검증

```
서버 측 검증 로직:
  stored_challenge = DB에서 code와 연관된 code_challenge 조회
  computed_challenge = BASE64URL(SHA256(code_verifier))

  if (stored_challenge != computed_challenge) {
    return error("invalid_grant", "PKCE 검증 실패")
  }
```

#### 단계 14: 토큰 응답

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImtleS0wMDEifQ...",
  "token_type": "Bearer",
  "expires_in": 900,
  "refresh_token": "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "scope": "openid profile email"
}
```

---

## 3. 토큰 상세

### 3.1 Access Token 구조

JWT(JSON Web Token) 형식으로 RS256 알고리즘으로 서명된다.

#### 헤더

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-001"
}
```

#### 페이로드 (클레임)

```json
{
  "iss": "https://sso.example.com",
  "sub": "user-uuid",
  "aud": "my-client",
  "exp": 1709470200,
  "iat": 1709469300,
  "jti": "token-uuid",
  "scope": "openid profile email",
  "roles": ["USER"],
  "client_id": "my-client"
}
```

#### 클레임 설명

| 클레임 | 설명 |
|--------|------|
| `iss` | 발급자 (SSO Server URL) |
| `sub` | 사용자 식별자 (UUID) |
| `aud` | 대상자 (Client ID) |
| `exp` | 만료 시간 (Unix Timestamp) |
| `iat` | 발급 시간 (Unix Timestamp) |
| `jti` | 토큰 고유 식별자 (UUID) |
| `scope` | 허용된 스코프 |
| `roles` | 사용자 역할 목록 |

### 3.2 ID Token 구조

OpenID Connect 표준에 따른 ID Token:

```json
{
  "iss": "https://sso.example.com",
  "sub": "user-uuid",
  "aud": "my-client",
  "exp": 1709470200,
  "iat": 1709469300,
  "auth_time": 1709469200,
  "nonce": "random-nonce-value",
  "name": "홍길동",
  "email": "hong@example.com",
  "email_verified": true,
  "preferred_username": "hong"
}
```

### 3.3 Refresh Token

Refresh Token은 불투명(opaque) 토큰 형식이며, 서버 측에 저장된다:

- 형식: URL-safe 랜덤 문자열 (128비트)
- 저장: DB에 해시값으로 저장
- 1회 사용: 갱신 시 새 Refresh Token 발급 (Rotation)

---

## 4. 토큰 생명주기

### 4.1 토큰 발급

```
인가 코드 교환 성공
     │
     ├── Access Token 생성 (JWT, RS256 서명)
     │     TTL: 15분 (CC 모드)
     │
     ├── ID Token 생성 (JWT, RS256 서명)
     │     TTL: 15분 (CC 모드)
     │
     └── Refresh Token 생성 (Opaque)
           TTL: 1시간 (CC 모드)
```

### 4.2 토큰 갱신 (Refresh)

```
  클라이언트                          SSO Server
     │                                   │
     │  POST /oauth2/token               │
     │  grant_type=refresh_token          │
     │  refresh_token={token}             │
     │──────────────────────────────────>│
     │                                   │
     │                   Refresh Token 검증│
     │                   - 존재 여부       │
     │                   - 만료 여부       │
     │                   - 사용 여부       │
     │                   - 클라이언트 확인  │
     │                                   │
     │                   이전 Refresh Token│
     │                   즉시 무효화       │
     │                   (Rotation)        │
     │                                   │
     │  새 토큰 세트 발급                  │
     │  - 새 Access Token                 │
     │  - 새 ID Token                     │
     │  - 새 Refresh Token                │
     │<──────────────────────────────────│
     │                                   │
```

### 4.3 토큰 폐기 (Revocation)

```
  클라이언트                          SSO Server
     │                                   │
     │  POST /oauth2/revoke              │
     │  token={access_or_refresh_token}  │
     │──────────────────────────────────>│
     │                                   │
     │                   토큰 블랙리스트   │
     │                   등록              │
     │                                   │
     │                   관련 Refresh Token│
     │                   도 함께 폐기      │
     │                                   │
     │                   감사 로그 기록     │
     │                                   │
     │  200 OK                           │
     │<──────────────────────────────────│
```

### 4.4 토큰 검증

API 요청 시 Bearer Token 검증 절차:

```
1. Authorization 헤더에서 JWT 추출
2. JWT 헤더의 kid로 공개키 조회
3. RS256 서명 검증
4. 클레임 유효성 검증:
   - iss: 발급자 일치 여부
   - exp: 만료 시간 초과 여부
   - aud: 대상자 일치 여부 (선택)
5. 블랙리스트 확인 (폐기된 토큰 여부)
6. 검증 성공: 요청 처리
   검증 실패: 401 Unauthorized
```

---

## 5. 세션과 토큰의 관계

### 5.1 SSO 세션

SSO Server는 사용자 인증 시 SSO 세션을 생성한다:

```
사용자 로그인
     │
     ├── SSO 세션 생성 (서버 측 In-Memory)
     │     - sessionId: UUID
     │     - userId: 사용자 ID
     │     - createdAt: 생성 시간
     │     - lastAccessedAt: 마지막 활동 시간
     │     - status: ACTIVE
     │
     └── 세션 쿠키 발급 (브라우저)
           - Name: AUTHFUSION_SESSION
           - HttpOnly: true
           - Secure: true (CC 모드)
           - SameSite: Lax
```

### 5.2 동일 세션에서의 추가 인가

사용자가 이미 SSO 세션을 가지고 있는 경우, 다른 클라이언트의 인가 요청 시 로그인 페이지를 건너뛴다:

```
클라이언트 B에서 /oauth2/authorize 요청
     │
     ├── 세션 쿠키 확인 → SSO 세션 유효
     │
     ├── 로그인 페이지 건너뜀 (SSO)
     │
     └── 인가 코드 즉시 발급
```

### 5.3 세션 종료 시 토큰 처리

SSO 세션이 종료되면 해당 세션에서 발급된 모든 토큰이 영향을 받는다:

| 종료 유형 | Access Token | Refresh Token |
|----------|:----------:|:------------:|
| 사용자 로그아웃 | 블랙리스트 등록 | 즉시 폐기 |
| 세션 타임아웃 | 만료 시 갱신 불가 | 폐기 |
| 관리자 강제 종료 | 블랙리스트 등록 | 즉시 폐기 |

---

## 6. OIDC Discovery

### 6.1 Discovery 엔드포인트

SSO Server는 OIDC Discovery 메타데이터를 제공한다:

```
GET /.well-known/openid-configuration
```

```json
{
  "issuer": "https://sso.example.com",
  "authorization_endpoint": "https://sso.example.com/oauth2/authorize",
  "token_endpoint": "https://sso.example.com/oauth2/token",
  "userinfo_endpoint": "https://sso.example.com/oauth2/userinfo",
  "revocation_endpoint": "https://sso.example.com/oauth2/revoke",
  "jwks_uri": "https://sso.example.com/.well-known/jwks.json",
  "response_types_supported": ["code"],
  "grant_types_supported": [
    "authorization_code",
    "refresh_token",
    "client_credentials"
  ],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256"],
  "scopes_supported": ["openid", "profile", "email", "roles", "offline_access"],
  "token_endpoint_auth_methods_supported": [
    "client_secret_basic",
    "client_secret_post",
    "none"
  ],
  "code_challenge_methods_supported": ["S256"]
}
```

### 6.2 JWKS 엔드포인트

```
GET /.well-known/jwks.json
```

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "key-001",
      "alg": "RS256",
      "n": "0vx7agoebGcQSuuPiLJXZ...",
      "e": "AQAB"
    }
  ]
}
```

---

## 7. 보안 고려사항

### 7.1 PKCE 필수

- `S256` 방식만 허용
- `code_challenge` 없는 인가 요청은 거부

### 7.2 State 매개변수

- CSRF 공격 방지를 위해 `state` 매개변수 사용 권장
- 클라이언트 측에서 검증 필수

### 7.3 Nonce 매개변수

- ID Token 재전송 공격 방지
- 인가 요청 시 전달한 `nonce`가 ID Token에 포함되는지 클라이언트에서 검증

### 7.4 Refresh Token Rotation

- Refresh Token은 1회 사용 후 새 토큰으로 교체
- 이미 사용된 Refresh Token으로 갱신 시도 시 관련 토큰 모두 폐기 (탈취 감지)

### 7.5 토큰 저장 권장 사항

| 클라이언트 유형 | Access Token | Refresh Token |
|----------------|-------------|---------------|
| 서버 앱 | 서버 메모리/세션 | 서버 메모리/세션 |
| SPA | 메모리 (변수) | 메모리 (변수) |
| 모바일 앱 | Secure Storage | Secure Storage |

> **경고**: LocalStorage에 토큰을 저장하지 않는다 (XSS 취약).

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
