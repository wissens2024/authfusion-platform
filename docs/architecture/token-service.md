# Token Service 아키텍처

## AuthFusion SSO Server v1.0

---

## 1. 개요

Token Service는 OIDC(OpenID Connect) 프로토콜에 따른 토큰의 전체 수명주기를 관리한다.
JWT 토큰의 생성, 서명, 검증, 갱신, 폐기와 RSA 서명키의 생성, 암호화 보관, 로테이션, 분배를 담당한다.

### 1.1 관련 SFR

| SFR | 명칭 | Token Service 구현 |
|-----|------|-------------------|
| FCS_COP.1 | 암호 연산 | JWT 서명(RS256), TOTP 비밀키 암호화(AES-256-GCM) |
| FCS_CKM.1 | 암호키 생성 | RSA 2048-bit 키 페어 생성 |
| FCS_CKM.2 | 암호키 분배 | JWKS 엔드포인트를 통한 공개키 분배 |
| FCS_CKM.4 | 암호키 파기 | 키 로테이션 시 이전 키 비활성화 |

### 1.2 구성 모듈

```
Token Service
├── jwt/
│   ├── JwtTokenProvider.java       # JWT 토큰 생성/서명 (@ToeScope)
│   ├── JwtTokenParser.java         # JWT 토큰 파싱/검증 (@ToeScope)
│   ├── JwkProvider.java            # JWK 형식 공개키 변환 (@ToeScope)
│   ├── KeyPairManager.java         # RSA 키 페어 관리 (@ToeScope)
│   ├── KeyEncryptionService.java   # AES-256-GCM 키 암호화 (@ToeScope)
│   ├── TokenClaims.java            # JWT 클레임 데이터 모델 (@ToeScope)
│   ├── entity/
│   │   └── SigningKeyEntity.java   # 서명키 영속화 엔티티 (@ToeScope)
│   └── repository/
│       └── SigningKeyRepository.java # 서명키 JPA 리포지토리 (@ToeScope)
├── oidc/
│   ├── endpoint/
│   │   ├── TokenEndpoint.java       # /oauth2/token (@ToeScope)
│   │   ├── JwksEndpoint.java        # /.well-known/jwks.json (@ToeScope)
│   │   ├── RevocationEndpoint.java  # /oauth2/revoke (@ToeScope)
│   │   └── DiscoveryEndpoint.java   # /.well-known/openid-configuration (@ToeScope)
│   ├── service/
│   │   ├── TokenService.java        # 토큰 통합 서비스 (@ToeScope)
│   │   ├── PkceValidator.java       # PKCE 검증 (@ToeScope)
│   │   └── ScopeService.java        # 스코프 관리 (@ToeScope)
│   └── grant/
│       ├── GrantHandler.java        # 그랜트 핸들러 인터페이스
│       ├── AuthorizationCodeGrantHandler.java  # AuthCode+PKCE (@ToeScope)
│       ├── RefreshTokenGrantHandler.java       # Refresh Token (@ToeScope)
│       └── ClientCredentialsGrantHandler.java  # Client Credentials (@ExtendedFeature)
```

---

## 2. 지원 Grant 타입

### 2.1 Authorization Code + PKCE (TOE)

사용자 인증을 위한 핵심 그랜트 타입이다. PKCE(Proof Key for Code Exchange)를 필수로 사용하여
인가 코드 탈취 공격을 방어한다.

**PKCE 검증 과정:**

```
1. 클라이언트: code_verifier (43~128자 랜덤 문자열) 생성
2. 클라이언트: code_challenge = BASE64URL(SHA256(code_verifier))
3. 인가 요청: code_challenge + code_challenge_method=S256 전송
4. 서버: code_challenge를 인가 코드와 함께 DB 저장
5. 토큰 요청: code_verifier 전송
6. 서버: BASE64URL(SHA256(code_verifier)) == 저장된 code_challenge 검증
```

**인가 코드 저장:**

```sql
INSERT INTO sso_authorization_codes
    (code, client_id, user_id, redirect_uri, scope,
     code_challenge, code_challenge_method, nonce, state, expires_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
```

| 필드 | 설명 |
|------|------|
| `code` | 인가 코드 (SecureRandom 기반 UUID) |
| `code_challenge` | PKCE challenge (SHA-256 해시) |
| `code_challenge_method` | 항상 `S256` |
| `nonce` | OIDC nonce (ID Token replay 방지) |
| `expires_at` | 만료 시각 (기본 10분) |

### 2.2 Refresh Token (TOE)

Access Token 만료 시 새 토큰을 발급하기 위한 그랜트 타입이다.

**처리 과정:**

```
1. POST /oauth2/token (grant_type=refresh_token)
2. Refresh Token JWT 서명 검증
3. 만료 시간 확인
4. 사용자 상태 확인 (ACTIVE)
5. 새 Access Token + 새 Refresh Token 발급
6. (선택) 이전 Refresh Token 폐기 (Rotation)
```

### 2.3 Client Credentials (@ExtendedFeature)

서비스 간 통신(M2M)을 위한 그랜트 타입이다. CC 모드에서는 비활성화된다.

```
1. POST /oauth2/token (grant_type=client_credentials)
2. client_id + client_secret 검증
3. Access Token만 발급 (ID Token/Refresh Token 없음)
```

---

## 3. JWT 토큰 구조

### 3.1 Access Token

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "<key-id>"
  },
  "payload": {
    "iss": "https://sso.example.com",
    "sub": "<user-uuid>",
    "aud": ["<client-id>"],
    "exp": 1709530800,
    "iat": 1709530500,
    "jti": "<token-uuid>",
    "scope": "openid profile email",
    "client_id": "<client-id>",
    "token_type": "access_token",
    "roles": ["USER", "ADMIN"],
    "preferred_username": "john.doe",
    "email": "john.doe@example.com"
  }
}
```

### 3.2 ID Token

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "<key-id>"
  },
  "payload": {
    "iss": "https://sso.example.com",
    "sub": "<user-uuid>",
    "aud": ["<client-id>"],
    "exp": 1709534100,
    "iat": 1709530500,
    "jti": "<token-uuid>",
    "token_type": "id_token",
    "nonce": "<client-nonce>",
    "preferred_username": "john.doe",
    "email": "john.doe@example.com",
    "email_verified": true,
    "given_name": "John",
    "family_name": "Doe",
    "roles": ["USER"]
  }
}
```

### 3.3 Refresh Token

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "<key-id>"
  },
  "payload": {
    "iss": "https://sso.example.com",
    "sub": "<user-uuid>",
    "exp": 1709616900,
    "iat": 1709530500,
    "jti": "<token-uuid>",
    "scope": "openid profile email",
    "client_id": "<client-id>",
    "token_type": "refresh_token"
  }
}
```

### 3.4 토큰 유효기간

| 토큰 유형 | 기본값 | CC 모드 | 설정 키 |
|-----------|--------|---------|---------|
| Access Token | 300초 (5분) | 300초 (5분) | `jwt.access-token-validity` |
| ID Token | 3,600초 (1시간) | 3,600초 (1시간) | `jwt.id-token-validity` |
| Refresh Token | 86,400초 (24시간) | **43,200초** (12시간) | `jwt.refresh-token-validity` |
| Authorization Code | 600초 (10분) | 600초 (10분) | `jwt.authorization-code-validity` |

---

## 4. RSA 키 관리

### 4.1 키 아키텍처 개요

```
┌─────────────────────────────────────────────────────────────────┐
│                    KeyPairManager                                │
│                                                                  │
│  ┌────────────────────┐    ┌──────────────────────────────────┐ │
│  │  activeKeyPair      │    │  keyPairCache (ConcurrentHashMap)│ │
│  │  activeKid          │    │  kid → KeyPair                   │ │
│  │                     │    │  (활성 키 + 이전 키 모두 보관)   │ │
│  └─────────┬──────────┘    └──────────────┬───────────────────┘ │
│            │                               │                     │
│  서명 시:   │  검증 시:                      │                     │
│  활성 키    │  kid로 공개키 조회              │                     │
│  사용       │  (캐시 → DB 폴백)              │                     │
│            │                               │                     │
└────────────┼───────────────────────────────┼─────────────────────┘
             │                               │
    ┌────────▼────────┐            ┌─────────▼───────────┐
    │ KeyEncryption   │            │ SigningKeyRepository │
    │ Service         │            │ (JPA)                │
    │ (AES-256-GCM)   │            │                      │
    └─────────────────┘            └──────────┬───────────┘
                                              │
                                    ┌─────────▼───────────┐
                                    │ PostgreSQL           │
                                    │ sso_signing_keys     │
                                    └─────────────────────┘
```

### 4.2 키 생성 (FCS_CKM.1)

`KeyPairManager.rotateKeyPair()` 메소드가 RSA 키 페어를 생성한다.

```
1. KeyPairGenerator.getInstance("RSA")
2. generator.initialize(2048)          // RSA 2048-bit
3. KeyPair newKeyPair = generator.generateKeyPair()
4. String newKid = UUID.randomUUID()   // Key ID 생성
```

**키 매개변수:**

| 항목 | 값 | 비고 |
|------|-----|------|
| 알고리즘 | RSA | Java KeyPairGenerator |
| 키 크기 | 2048-bit | NIST 권고 최소 2048-bit |
| Key ID (kid) | UUID v4 | JWT 헤더에 포함 |
| 서명 알고리즘 | RS256 | RSA + SHA-256 |

### 4.3 키 암호화 보관 (FCS_COP.1)

`KeyEncryptionService`가 RSA 비밀키를 AES-256-GCM으로 암호화하여 DB에 저장한다.

**마스터 키 파생:**

```
masterSecret (환경변수 AUTHFUSION_KEY_MASTER_SECRET)
    │
    ▼
SHA-256(masterSecret.getBytes(UTF-8))
    │
    ▼
256-bit AES SecretKey
```

**암호화 과정:**

```
1. SecureRandom으로 12-byte IV 생성
2. Cipher.getInstance("AES/GCM/NoPadding")
3. cipher.init(ENCRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
4. encryptedBytes = cipher.doFinal(privateKeyPem)
5. DB 저장: Base64(encryptedBytes) + Base64(iv)
```

**AES-256-GCM 매개변수:**

| 항목 | 값 | 비고 |
|------|-----|------|
| 암호 알고리즘 | AES/GCM/NoPadding | 인증 암호화 |
| 키 크기 | 256-bit | SHA-256 파생 |
| IV 크기 | 12-byte (96-bit) | GCM 표준 |
| 인증 태그 크기 | 128-bit | 무결성 보장 |

### 4.4 키 저장 스키마

`sso_signing_keys` 테이블 구조:

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `id` | UUID | PK |
| `kid` | VARCHAR (UNIQUE) | Key ID (JWT 헤더 참조) |
| `algorithm` | VARCHAR | 서명 알고리즘 (RS256) |
| `key_size` | INTEGER | 키 크기 (2048) |
| `public_key` | TEXT | Base64 인코딩된 공개키 (X.509) |
| `encrypted_private_key` | TEXT | AES-256-GCM 암호화된 비밀키 |
| `iv` | VARCHAR(64) | Base64 인코딩된 GCM IV |
| `active` | BOOLEAN | 현재 활성 키 여부 |
| `expires_at` | TIMESTAMP | 키 만료 시각 |
| `created_at` | TIMESTAMP | 생성 시각 |
| `rotated_at` | TIMESTAMP | 비활성화 시각 |

### 4.5 키 로테이션

**자동 로테이션:**

```
서버 기동 시 (@PostConstruct):
    │
    ▼
DB에서 활성 키 조회 (findByActiveTrue)
    │
    ├── 활성 키 있음 → 만료 확인
    │     │
    │     ├── 만료됨 → rotateKeyPair() 호출
    │     │
    │     └── 유효함 → 로드 완료
    │
    └── 활성 키 없음 → rotateKeyPair() 호출 (최초 기동)
```

**rotateKeyPair() 절차:**

```
1. 새 RSA 키 페어 생성 (2048-bit)
2. 기존 활성 키 비활성화 (active=false, rotatedAt=now)
3. 새 비밀키 AES-256-GCM 암호화
4. DB에 새 키 저장 (active=true, expiresAt=now+90일)
5. 메모리 캐시 업데이트 (activeKeyPair, activeKid)
6. 이전 키는 캐시에 유지 (검증용)
```

**로테이션 주기:**

| 설정 | 값 | 비고 |
|------|-----|------|
| `key-rotation-days` | 90일 | NIST SP 800-57 참고 |

### 4.6 키 분배 (FCS_CKM.2)

JWKS(JSON Web Key Set) 엔드포인트를 통해 공개키를 분배한다.

**엔드포인트**: `GET /.well-known/jwks.json`

```json
{
  "keys": [
    {
      "kty": "RSA",
      "kid": "<key-id>",
      "use": "sig",
      "alg": "RS256",
      "n": "<modulus-base64url>",
      "e": "<exponent-base64url>"
    }
  ]
}
```

- 활성 키와 이전 키의 공개키를 모두 포함
- 이전 키를 포함함으로써 로테이션 전에 발급된 토큰도 검증 가능
- 비밀키는 절대 노출하지 않음

### 4.7 키 파기 (FCS_CKM.4)

키 로테이션 시 이전 키는 `active=false`로 설정되어 새 서명에 사용되지 않는다.
이전 키는 기발급 토큰 검증을 위해 일정 기간 유지된다.

```
키 수명주기:
  [생성] → [활성(서명+검증)] → [비활성(검증만)] → [만료(삭제)]
               ▲                    ▲                  ▲
           rotateKeyPair()     다음 로테이션      토큰 만료 후 정리
```

---

## 5. JWT 서명/검증 과정

### 5.1 JWT 서명 (`JwtTokenProvider`)

```
TokenClaims (클레임 데이터)
    │
    ▼
JWTClaimsSet 구성
  - sub, iss, aud, exp, iat, jti
  - scope, client_id, token_type
  - roles, preferred_username, email
    │
    ▼
JWSHeader 구성
  - alg: RS256
  - kid: activeKid (활성 키 ID)
  - typ: JWT
    │
    ▼
SignedJWT(header, claimsSet)
    │
    ▼
RSASSASigner(privateKey) 서명
    │
    ▼
signedJWT.serialize()
  → "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ii4uLiJ9.eyJz..."
```

### 5.2 JWT 검증 (`JwtTokenParser`)

```
JWT 문자열
    │
    ▼
SignedJWT.parse(jwt)
    │
    ▼
JWSHeader에서 kid 추출
    │
    ▼
KeyPairManager.getPublicKeyByKid(kid)
  - keyPairCache 조회 → 없으면 DB 조회
    │
    ▼
RSASSAVerifier(publicKey) 검증
    │
    ├── 서명 불일치 → 토큰 거부
    │
    └── 서명 일치 → 클레임 검증
          │
          ├── exp < now → 토큰 만료
          ├── iss != issuer → 발급자 불일치
          └── 유효 → TokenClaims 반환
```

---

## 6. OIDC Discovery

`GET /.well-known/openid-configuration` 엔드포인트는 OIDC 표준 메타데이터를 제공한다.

```json
{
  "issuer": "https://sso.example.com",
  "authorization_endpoint": "https://sso.example.com/oauth2/authorize",
  "token_endpoint": "https://sso.example.com/oauth2/token",
  "userinfo_endpoint": "https://sso.example.com/oauth2/userinfo",
  "jwks_uri": "https://sso.example.com/.well-known/jwks.json",
  "revocation_endpoint": "https://sso.example.com/oauth2/revoke",
  "response_types_supported": ["code"],
  "grant_types_supported": ["authorization_code", "refresh_token"],
  "subject_types_supported": ["public"],
  "id_token_signing_alg_values_supported": ["RS256"],
  "scopes_supported": ["openid", "profile", "email", "offline_access"],
  "code_challenge_methods_supported": ["S256"]
}
```

---

## 7. 토큰 폐기

`POST /oauth2/revoke` 엔드포인트를 통해 토큰을 명시적으로 폐기할 수 있다.

**폐기 과정:**

```
1. POST /oauth2/revoke
   Content-Type: application/x-www-form-urlencoded
   token=<access_token_or_refresh_token>
   &token_type_hint=access_token  (또는 refresh_token)

2. 토큰 타입 판별
3. Refresh Token인 경우: DB에서 삭제
4. Access Token인 경우: 블랙리스트 등록
5. 감사 로그 기록 (TOKEN_OPERATION / TOKEN_REVOKED)
6. 200 OK 응답
```

---

## 8. SSO Agent의 토큰 검증

SSO Agent는 독립적으로 JWT 토큰을 검증한다.

### 8.1 Agent 토큰 검증 흐름

```
HTTP 요청 (Authorization 헤더 또는 SSO 쿠키)
    │
    ▼
SsoAuthenticationFilter
    │
    ▼
JwtTokenValidator.validate(token)
    │
    ├── TokenCache에서 검증 결과 캐시 확인
    │     ├── 캐시 히트 → 캐시된 결과 반환
    │     └── 캐시 미스 → 검증 수행
    │
    ▼
JwksKeyResolver.resolve(kid)
    │
    ├── 로컬 캐시 확인
    │     ├── 캐시 히트 → 공개키 반환
    │     └── 캐시 미스 → SSO Server JWKS 엔드포인트 조회
    │           GET /.well-known/jwks.json
    │           → 공개키 캐시 갱신
    │
    ▼
RSA 서명 검증 + 만료/issuer 확인
    │
    ▼
TokenInfo (sub, roles, scope 등) → SsoSecurityContext 설정
```

### 8.2 Agent JWKS 캐싱

- JWKS 공개키는 로컬에 캐싱하여 SSO Server 요청 최소화
- 미지의 kid 등장 시 자동으로 JWKS 재조회
- 캐시 만료 시간은 `SsoAgentProperties`에서 설정 가능

---

## 9. 보안 고려사항

### 9.1 키 보안

- RSA 비밀키 평문은 DB에 절대 저장하지 않음
- 마스터 시크릿은 환경변수로 주입 (소스코드에 포함 금지)
- 기본 마스터 시크릿(`authfusion-default-master-key-change-me-in-production`)은 개발용이며, 프로덕션에서 사용 시 경고 로그 출력
- HSM 전환 시 `KeyEncryptionService`를 HSM 인터페이스로 교체 가능

### 9.2 토큰 보안

- PKCE 필수 (code_challenge_method=S256)
- Access Token 유효기간 최소화 (5분)
- Refresh Token 회전(Rotation) 지원
- 토큰에 jti(JWT ID) 포함으로 고유성 보장
- nonce를 통한 ID Token replay 공격 방지

### 9.3 알고리즘 선택

| 용도 | 알고리즘 | 강도 | 비고 |
|------|---------|------|------|
| JWT 서명 | RS256 (RSA-PKCS1-v1.5 + SHA-256) | 112-bit equiv. | NIST 2030년까지 유효 |
| 키 암호화 | AES-256-GCM | 256-bit | NIST 승인 AEAD |
| 마스터 키 파생 | SHA-256 | 256-bit | 단방향 해시 |
| 비밀번호 해싱 | BCrypt (cost=12) | - | 적응형 해시 |

---

## 10. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
