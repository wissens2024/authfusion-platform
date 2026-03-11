# 기능 명세 (ADV_FSP)

## TOE 보안 기능 인터페이스 (TSFI) 명세

---

## 1. 개요

본 문서는 AuthFusion SSO Server의 TSFI(TOE Security Functions Interface)를 기술한다.
각 보안 기능 인터페이스의 목적, 사용 방법, 매개변수, 동작, 오류 처리를 명세한다.

### 1.1 TSFI 분류

| 분류 | 인터페이스 수 | 설명 |
|------|-------------|------|
| OIDC 인가 인터페이스 | 3 | 인가 코드 발급, 토큰 교환 |
| 토큰 관리 인터페이스 | 4 | 토큰 발급/검증/폐기/갱신 |
| 사용자 인증 인터페이스 | 2 | 로그인/로그아웃 |
| 세션 관리 인터페이스 | 3 | 세션 조회/종료 |
| 감사 로그 인터페이스 | 1 | 감사 이벤트 조회 |
| 암호키 관리 인터페이스 | 2 | JWKS 조회, 키 로테이션 |

---

## 2. OIDC 인가 인터페이스

### 2.1 TSFI-AUTHZ-001: 인가 요청 (Authorization Request)

- **인터페이스**: `GET /oauth2/authorize`
- **SFR**: FIA_UAU.1, FDP_ACC.1
- **목적**: OIDC Authorization Code + PKCE 흐름의 인가 요청을 처리한다

#### 요청 매개변수

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `response_type` | String | Y | `code` (고정) |
| `client_id` | String | Y | 등록된 OAuth2 클라이언트 ID |
| `redirect_uri` | String | Y | 인가 코드 전달 URI |
| `scope` | String | Y | 요청 스코프 (예: `openid profile email`) |
| `state` | String | Y | CSRF 방지 상태값 |
| `code_challenge` | String | Y | PKCE 코드 챌린지 (S256) |
| `code_challenge_method` | String | Y | `S256` (고정) |
| `nonce` | String | N | 재전송 공격 방지 (ID Token에 포함) |

#### 동작 흐름

1. `client_id`로 등록된 클라이언트 조회
2. `redirect_uri`가 등록된 URI와 일치하는지 검증
3. 요청 스코프 유효성 검증
4. `code_challenge_method`가 `S256`인지 확인
5. 사용자 인증 상태 확인 (미인증 시 로그인 페이지로 리다이렉트)
6. 인증된 경우 인가 코드 생성 및 `redirect_uri`로 리다이렉트

#### 응답

- **성공**: `302 Redirect` -> `{redirect_uri}?code={authorization_code}&state={state}`
- **실패**: `302 Redirect` -> `{redirect_uri}?error={error_code}&error_description={message}`

#### 오류 코드

| 오류 | 설명 |
|------|------|
| `invalid_request` | 필수 매개변수 누락 또는 잘못된 형식 |
| `unauthorized_client` | 클라이언트가 해당 그랜트 타입 미지원 |
| `invalid_scope` | 요청 스코프가 유효하지 않음 |
| `access_denied` | 사용자가 인가 거부 |

### 2.2 TSFI-AUTHZ-002: 토큰 요청 (Token Request)

- **인터페이스**: `POST /oauth2/token`
- **SFR**: FIA_UAU.1, FCS_COP.1
- **Content-Type**: `application/x-www-form-urlencoded`
- **목적**: 인가 코드를 Access Token/ID Token으로 교환한다

#### 요청 매개변수 (Authorization Code Grant)

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `grant_type` | String | Y | `authorization_code` |
| `code` | String | Y | 인가 코드 |
| `redirect_uri` | String | Y | 인가 요청 시 사용한 URI와 동일 |
| `client_id` | String | Y | 클라이언트 ID |
| `client_secret` | String | 조건부 | Confidential 클라이언트인 경우 필수 |
| `code_verifier` | String | Y | PKCE 코드 검증자 |

#### 요청 매개변수 (Refresh Token Grant)

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `grant_type` | String | Y | `refresh_token` |
| `refresh_token` | String | Y | 리프레시 토큰 |
| `client_id` | String | Y | 클라이언트 ID |
| `client_secret` | String | 조건부 | Confidential 클라이언트인 경우 필수 |
| `scope` | String | N | 축소된 스코프 (원래 범위 이하) |

#### 응답 (성공, 200 OK)

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIs...",
  "token_type": "Bearer",
  "expires_in": 900,
  "refresh_token": "dGhpcyBpcyBhIHJlZnJl...",
  "id_token": "eyJhbGciOiJSUzI1NiIs...",
  "scope": "openid profile email"
}
```

#### PKCE 검증 로직

```
1. code_verifier를 SHA-256으로 해싱
2. Base64url 인코딩
3. 인가 요청 시 저장된 code_challenge와 비교
4. 불일치 시 invalid_grant 오류 반환
```

#### 오류 코드

| 오류 | 설명 |
|------|------|
| `invalid_grant` | 인가 코드 만료/사용됨, PKCE 검증 실패 |
| `invalid_client` | 클라이언트 인증 실패 |
| `unsupported_grant_type` | 미지원 그랜트 타입 |

### 2.3 TSFI-AUTHZ-003: 사용자 정보 조회 (UserInfo)

- **인터페이스**: `GET /oauth2/userinfo`
- **SFR**: FIA_UAU.1, FDP_ACF.1
- **인증**: Bearer Token (Access Token)
- **목적**: Access Token에 연관된 사용자 정보를 반환한다

#### 응답 (성공, 200 OK)

```json
{
  "sub": "user-uuid",
  "name": "홍길동",
  "email": "hong@example.com",
  "email_verified": true,
  "roles": ["USER"]
}
```

---

## 3. 토큰 관리 인터페이스

### 3.1 TSFI-TOKEN-001: 토큰 폐기 (Revocation)

- **인터페이스**: `POST /oauth2/revoke`
- **SFR**: FIA_UAU.1
- **Content-Type**: `application/x-www-form-urlencoded`
- **목적**: 발급된 토큰을 즉시 폐기한다

#### 요청 매개변수

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `token` | String | Y | 폐기할 토큰 값 |
| `token_type_hint` | String | N | `access_token` 또는 `refresh_token` |
| `client_id` | String | Y | 클라이언트 ID |
| `client_secret` | String | 조건부 | Confidential 클라이언트 |

#### 동작

1. 토큰 소유 클라이언트 확인
2. 토큰을 블랙리스트에 추가
3. 관련 Refresh Token도 함께 폐기
4. 감사 이벤트 `TOKEN_REVOKED` 기록

#### 응답

- **성공**: `200 OK` (빈 본문, RFC 7009 준수)
- **실패**: `401 Unauthorized` (클라이언트 인증 실패)

### 3.2 TSFI-TOKEN-002: JWT 서명 검증

- **인터페이스**: 내부 API (JwtTokenParser)
- **SFR**: FCS_COP.1
- **목적**: JWT 토큰의 RS256 서명을 검증한다

#### 검증 절차

1. JWT 헤더에서 `kid`(Key ID) 추출
2. `kid`에 해당하는 공개키를 KeyPairManager에서 조회
3. RS256 서명 검증 (Nimbus JOSE+JWT 라이브러리)
4. 클레임 유효성 검증 (만료 시간, 발급자, 대상자)
5. 토큰 블랙리스트 확인

---

## 4. 사용자 인증 인터페이스

### 4.1 TSFI-AUTH-001: 로그인

- **인터페이스**: `POST /api/v1/auth/login`
- **SFR**: FIA_UAU.1, FIA_UID.1, FIA_AFL.1
- **Content-Type**: `application/json`
- **목적**: 사용자 자격증명을 검증하여 인증한다

#### 요청

```json
{
  "username": "사용자ID",
  "password": "비밀번호"
}
```

#### 동작 흐름

1. 계정 잠금 상태 확인 (FIA_AFL.1)
2. 사용자 식별 (FIA_UID.1)
3. 비밀번호 해시 비교 - BCrypt (FCS_COP.1)
4. 인증 성공 시:
   - SSO 세션 생성
   - 로그인 실패 카운터 초기화
   - 감사 이벤트 `AUTH_LOGIN_SUCCESS` 기록
5. 인증 실패 시:
   - 로그인 실패 카운터 증가
   - 임계값 도달 시 계정 잠금 (FIA_AFL.1)
   - 감사 이벤트 `AUTH_LOGIN_FAILURE` 기록

#### 응답 (성공, 200 OK)

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "dGhpcyBpcyBh...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "user-uuid",
    "username": "사용자ID",
    "roles": ["USER"]
  }
}
```

#### 오류 응답 (401 Unauthorized)

```json
{
  "error": "AUTHENTICATION_FAILED",
  "message": "사용자 이름 또는 비밀번호가 올바르지 않습니다."
}
```

### 4.2 TSFI-AUTH-002: 로그아웃

- **인터페이스**: `POST /api/v1/auth/logout`
- **SFR**: FTA_SSL.3
- **인증**: Bearer Token
- **목적**: 현재 세션을 종료하고 토큰을 폐기한다

#### 동작

1. 현재 세션 종료
2. Access Token 블랙리스트 등록
3. 관련 Refresh Token 폐기
4. 감사 이벤트 `AUTH_LOGOUT` 기록

---

## 5. 세션 관리 인터페이스

### 5.1 TSFI-SESSION-001: 세션 조회

- **인터페이스**: `GET /api/v1/sessions`
- **SFR**: FTA_SSL.3
- **인증**: Bearer Token (ADMIN 역할)
- **목적**: 활성 세션 목록을 조회한다

### 5.2 TSFI-SESSION-002: 세션 강제 종료

- **인터페이스**: `DELETE /api/v1/sessions/{sessionId}`
- **SFR**: FTA_SSL.3
- **인증**: Bearer Token (ADMIN 역할)
- **목적**: 지정된 세션을 강제 종료한다

#### 동작

1. 세션 ID로 활성 세션 조회
2. 세션 상태를 TERMINATED로 변경
3. 관련 토큰 폐기
4. 감사 이벤트 `SESSION_TERMINATED` 기록

### 5.3 TSFI-SESSION-003: 세션 타임아웃 (자동)

- **인터페이스**: 내부 스케줄러 (SessionService)
- **SFR**: FTA_SSL.3
- **목적**: 비활성 세션을 자동 종료한다

#### 동작

1. 마지막 활동 시간 + 타임아웃 시간 경과 여부 확인
2. 타임아웃 시 세션 상태를 EXPIRED로 변경
3. 감사 이벤트 `SESSION_EXPIRED` 기록

---

## 6. 감사 로그 인터페이스

### 6.1 TSFI-AUDIT-001: 감사 이벤트 조회

- **인터페이스**: `GET /api/v1/audit/events`
- **SFR**: FAU_SAR.1
- **인증**: Bearer Token (ADMIN 역할)
- **목적**: 기록된 감사 이벤트를 조회한다

#### 요청 매개변수

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `page` | Integer | N | 페이지 번호 (기본: 0) |
| `size` | Integer | N | 페이지 크기 (기본: 20) |
| `eventType` | String | N | 이벤트 유형 필터 |
| `username` | String | N | 사용자 필터 |
| `from` | DateTime | N | 시작 일시 |
| `to` | DateTime | N | 종료 일시 |
| `sort` | String | N | 정렬 기준 (기본: `createdAt,desc`) |

#### 응답 (200 OK)

```json
{
  "content": [
    {
      "id": "event-uuid",
      "eventType": "AUTH_LOGIN_SUCCESS",
      "username": "user01",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "details": "로그인 성공",
      "createdAt": "2026-03-03T10:30:00Z"
    }
  ],
  "totalElements": 1234,
  "totalPages": 62,
  "number": 0,
  "size": 20
}
```

---

## 7. 암호키 관리 인터페이스

### 7.1 TSFI-KEY-001: JWKS 공개키 조회

- **인터페이스**: `GET /.well-known/jwks.json`
- **SFR**: FCS_CKM.1, FCS_CKM.2
- **인증**: 없음 (공개 엔드포인트)
- **목적**: JWT 서명 검증을 위한 공개키 세트를 제공한다

#### 응답 (200 OK)

```json
{
  "keys": [
    {
      "kty": "RSA",
      "use": "sig",
      "kid": "key-id-001",
      "alg": "RS256",
      "n": "0vx7agoebGc...",
      "e": "AQAB"
    }
  ]
}
```

### 7.2 TSFI-KEY-002: 키 로테이션

- **인터페이스**: `POST /api/v1/keys/rotate` (내부 관리 API)
- **SFR**: FCS_CKM.1, FCS_CKM.4
- **인증**: Bearer Token (ADMIN 역할)
- **목적**: 새 RSA 키 페어를 생성하고 기존 키를 비활성화한다

#### 동작

1. 새 RSA-2048 키 페어 생성 (FCS_CKM.1)
2. 개인키를 마스터 키로 AES-256-GCM 암호화
3. DB에 저장 (상태: ACTIVE)
4. 이전 키 상태를 INACTIVE로 변경
5. 유예 기간(기본 24시간) 경과 후 이전 키 안전 삭제 (FCS_CKM.4)
6. 감사 이벤트 `KEY_ROTATED` 기록

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
