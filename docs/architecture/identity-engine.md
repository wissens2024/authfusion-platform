# Identity Engine 아키텍처

## AuthFusion SSO Server v1.0

---

## 1. 개요

Identity Engine은 SSO Server의 핵심 인증 모듈로, 사용자 식별(Identification)과 인증(Authentication)을 담당한다.
비밀번호 인증, TOTP 다중 인증(MFA), LDAP 외부 인증을 지원하며, 무차별 대입 공격 방어와 비밀번호 정책 관리를 포함한다.

### 1.1 관련 SFR

| SFR | 명칭 | Identity Engine 구현 |
|-----|------|---------------------|
| FIA_UAU.1 | 인증 타이밍 | 사용자 인증 수행 전 식별 요구 |
| FIA_UID.1 | 식별 타이밍 | 사용자명 기반 식별 |
| FIA_AFL.1 | 인증 실패 처리 | 로그인 실패 횟수 추적 및 계정 잠금 |
| FIA_SOS.1 | 비밀 검증 | 비밀번호 강도 정책 적용 |
| FIA_USB.1 | 사용자-주체 바인딩 | 인증된 사용자와 세션/토큰 바인딩 |

### 1.2 구성 모듈

```
Identity Engine
├── user/
│   ├── controller/
│   │   ├── AuthController.java          # 인증 API (@ToeScope)
│   │   └── UserController.java          # 사용자 관리 (@ExtendedFeature)
│   ├── service/
│   │   ├── UserService.java             # 인증 핵심 로직 (@ToeScope)
│   │   ├── PasswordHashService.java     # BCrypt 해싱 (@ToeScope)
│   │   └── PasswordPolicyService.java   # 비밀번호 정책 (@ToeScope)
│   ├── model/
│   │   ├── UserEntity.java              # 사용자 엔티티
│   │   ├── LoginRequest.java            # 로그인 요청 DTO
│   │   ├── LoginResponse.java           # 로그인 응답 DTO
│   │   └── PasswordHistoryEntity.java   # 비밀번호 이력
│   └── repository/
│       ├── UserRepository.java
│       └── PasswordHistoryRepository.java
├── security/
│   ├── BruteForceProtectionService.java # 무차별 대입 보호 (@ToeScope)
│   ├── RateLimitFilter.java             # 요청 빈도 제한 (@ToeScope)
│   └── LoginAttemptEntity.java          # 로그인 시도 기록
├── mfa/
│   ├── service/
│   │   ├── TotpService.java             # TOTP 핵심 (@ToeScope)
│   │   └── MfaSessionService.java       # MFA 대기 세션 (@ToeScope)
│   ├── model/
│   │   ├── TotpSecretEntity.java        # TOTP 비밀키 (암호화 저장)
│   │   ├── RecoveryCodeEntity.java      # 복구 코드 (BCrypt 해시)
│   │   └── MfaPendingSessionEntity.java # MFA 대기 세션
│   ├── controller/
│   │   └── MfaController.java           # MFA API (@ToeScope)
│   └── repository/
│       ├── TotpSecretRepository.java
│       ├── RecoveryCodeRepository.java
│       └── MfaPendingSessionRepository.java
└── session/
    ├── service/SessionService.java      # 세션 서비스 (@ToeScope)
    ├── store/
    │   ├── SessionStore.java            # 세션 저장소 인터페이스
    │   └── InMemorySessionStore.java    # 메모리 기반 구현
    └── model/
        ├── SsoSession.java              # 세션 객체
        ├── SessionStatus.java           # 세션 상태 (ACTIVE, EXPIRED, REVOKED)
        └── SessionInfo.java             # 세션 정보 DTO
```

---

## 2. 인증 방식

### 2.1 로컬 인증 (Username/Password)

로컬 사용자 저장소(PostgreSQL `sso_users` 테이블)에 저장된 비밀번호 해시와 대조하여 인증한다.

**비밀번호 해싱:**

| 항목 | 설정 |
|------|------|
| 알고리즘 | BCrypt |
| Cost Factor | 12 (2^12 = 4,096 라운드) |
| Salt | BCrypt 내장 랜덤 솔트 (22자) |
| 구현 | `PasswordEncoder` (Spring Security `BCryptPasswordEncoder`) |
| 검증 | `PasswordHashService.matches(rawPassword, hashedPassword)` |

**인증 절차:**

```
1. 사용자명으로 UserEntity 조회
2. 사용자 상태 확인 (ACTIVE 여부)
3. BruteForceProtectionService.isBlocked(username) 확인
4. BCrypt.matches(입력 비밀번호, 저장된 해시)
5. 성공 → BruteForceProtectionService.recordSuccessfulAttempt()
   실패 → BruteForceProtectionService.recordFailedAttempt()
         + AuditService.logAuthentication() (실패 기록)
6. 성공 시 → MFA 필요 여부 확인 → 세션/토큰 발급
```

### 2.2 TOTP 다중 인증 (RFC 6238)

시간 동기화 기반 일회용 비밀번호(TOTP)를 사용한 2차 인증이다.

**TOTP 매개변수:**

| 항목 | 기본값 | CC 모드 | 비고 |
|------|--------|---------|------|
| 알고리즘 | HMAC-SHA1 | HMAC-SHA1 | RFC 6238 표준 |
| 자릿수 | 6 | 6 | OTP 코드 길이 |
| 주기 | 30초 | 30초 | Time Step |
| 허용 윈도우 | +/-1 | +/-1 | 시간 오차 허용 범위 |
| 비밀키 길이 | 20바이트 | 20바이트 | Base32 인코딩 |

**TOTP 코드 생성 알고리즘 (RFC 6238):**

```
timeStep = floor(currentTime / period)  // 30초 단위
hmac = HMAC-SHA1(secretKey, timeStep)   // 8바이트 빅엔디안
offset = hmac[19] & 0x0F                // 마지막 바이트의 하위 4비트
binary = (hmac[offset] & 0x7F) << 24    // 4바이트 추출
       | (hmac[offset+1] & 0xFF) << 16
       | (hmac[offset+2] & 0xFF) << 8
       | (hmac[offset+3] & 0xFF)
otp = binary % 10^digits                // 6자리 코드
```

**비밀키 보관:**

- TOTP 비밀키는 `KeyEncryptionService`를 사용하여 AES-256-GCM으로 암호화
- `sso_totp_secrets` 테이블에 `encryptedSecret` + `iv` 형태로 저장
- 평문 비밀키는 DB에 절대 저장하지 않음

**복구 코드:**

- 생성 수: 10개 (기본)
- 형식: 8자 영숫자 (중간에 하이픈 포함, 예: `abc1-def2`)
- 저장: BCrypt 해시 (`sso_recovery_codes` 테이블)
- 사용: 1회용 (사용 후 `used=true`, `usedAt` 기록)
- TOTP 검증 실패 시 복구 코드로 대체 인증 가능

### 2.3 LDAP 인증 (Search-then-Bind)

외부 LDAP/Active Directory 디렉터리를 통한 인증을 지원한다.

**인증 방식: Search-then-Bind**

```
1. 서비스 계정(bind-dn)으로 LDAP 서버에 바인드
2. 사용자 검색: user-search-base + user-search-filter
   예: ou=users,dc=authfusion,dc=io 아래에서 (uid={username}) 검색
3. 검색 결과에서 사용자 DN 획득
4. 해당 DN + 입력 비밀번호로 재바인드 시도
5. 바인드 성공 → 인증 성공
6. LDAP 속성 → UserEntity 동기화
```

**LDAP 설정:**

```yaml
authfusion:
  sso:
    ldap:
      enabled: false                    # 기본 비활성화
      url: ldap://localhost:389         # LDAP 서버 (프로덕션: ldaps://...)
      base-dn: dc=authfusion,dc=io
      bind-dn: cn=sso-service,dc=authfusion,dc=io
      bind-password: ${AUTHFUSION_LDAP_BIND_PASSWORD:}
      user-search-base: ou=users
      user-search-filter: "(uid={0})"
      connect-timeout: 5000
      read-timeout: 10000
      attribute-mapping:
        username: uid
        email: mail
        first-name: givenName
        last-name: sn
```

**LDAP 사용자 동기화:**

```
LDAP 인증 성공 시:
  → LdapUserSyncService.syncUser(ldapAttributes)
  → sso_users 테이블에 동기화
  → UserEntity.source = "LDAP"
  → UserEntity.externalId = LDAP DN
  → UserEntity.ldapSyncedAt = now()
```

**보안 고려사항:**

- LDAP Injection 방지: 검색 필터의 특수문자 이스케이프 (`escapeFilter`)
- LDAPS(TLS) 연결 권장 (프로덕션 환경 필수)
- 바인드 비밀번호는 환경변수로 주입 (`AUTHFUSION_LDAP_BIND_PASSWORD`)
- JNDI(javax.naming) 기반 구현, 추가 라이브러리 불필요

---

## 3. 인증 플로우 상세

### 3.1 REST API 인증 (MFA 비활성화)

```
클라이언트                  AuthController              UserService
    │                          │                           │
    │ POST /api/v1/auth/login  │                           │
    │  { username, password }  │                           │
    │─────────────────────────>│                           │
    │                          │                           │
    │                          │ authenticate(user, pass)  │
    │                          │──────────────────────────>│
    │                          │                           │
    │                          │                           │ BruteForce 확인
    │                          │                           │ UserEntity 조회
    │                          │                           │ BCrypt 검증
    │                          │                           │ (또는 LDAP 인증)
    │                          │                           │
    │                          │  UserEntity (인증됨)       │
    │                          │<──────────────────────────│
    │                          │                           │
    │                          │ TotpService.isTotpEnabled()
    │                          │  → false                  │
    │                          │                           │
    │                          │ HttpSession 생성          │
    │                          │ AuditService.log()        │
    │                          │                           │
    │  200 OK                  │                           │
    │  { sessionId, user }     │                           │
    │<─────────────────────────│                           │
```

### 3.2 REST API 인증 (MFA 활성화)

```
클라이언트                  AuthController              TotpService
    │                          │                           │
    │ POST /api/v1/auth/login  │                           │
    │  { username, password }  │                           │
    │─────────────────────────>│                           │
    │                          │                           │
    │                          │ authenticate() → 성공     │
    │                          │                           │
    │                          │ isTotpEnabled() → true    │
    │                          │──────────────────────────>│
    │                          │                           │
    │                          │ MfaSessionService         │
    │                          │ .createPendingSession()   │
    │                          │                           │
    │  200 OK                  │                           │
    │  { mfaRequired: true,    │                           │
    │    mfaToken: "..." }     │                           │
    │<─────────────────────────│                           │
    │                          │                           │
    │ POST /api/v1/auth/       │                           │
    │   mfa/verify             │                           │
    │  ?mfaToken=...           │                           │
    │  { code: "123456" }      │                           │
    │─────────────────────────>│                           │
    │                          │                           │
    │                          │ validateAndGet(mfaToken)  │
    │                          │ verifyTotp(userId, code)  │
    │                          │──────────────────────────>│
    │                          │                           │
    │                          │  true (TOTP 또는 복구코드)│
    │                          │<──────────────────────────│
    │                          │                           │
    │                          │ consumePendingSession()   │
    │                          │ HttpSession 생성          │
    │                          │                           │
    │  200 OK                  │                           │
    │  { sessionId, user }     │                           │
    │<─────────────────────────│                           │
```

### 3.3 OIDC Form 로그인 + MFA

```
브라우저                       SSO Server                    DB
    │                              │                          │
    │ GET /oauth2/authorize        │                          │
    │   ?response_type=code        │                          │
    │   &client_id=...             │                          │
    │   &code_challenge=...        │                          │
    │──────────────────────────────>│                          │
    │                              │ 세션 없음 확인            │
    │                              │                          │
    │ 302 → /login?client_id=...   │                          │
    │<──────────────────────────────│                          │
    │                              │                          │
    │ GET /login?client_id=...     │                          │
    │──────────────────────────────>│                          │
    │ ← login.html (Thymeleaf)    │                          │
    │<──────────────────────────────│                          │
    │                              │                          │
    │ POST /login                  │                          │
    │   { username, password }     │                          │
    │──────────────────────────────>│                          │
    │                              │ 인증 수행                 │
    │                              │──────────────────────────>│
    │                              │<──────────────────────────│
    │                              │                          │
    │                              │ TOTP 활성화 확인          │
    │ 302 → /login/mfa?mfa_token=..│                          │
    │<──────────────────────────────│                          │
    │                              │                          │
    │ GET /login/mfa               │                          │
    │──────────────────────────────>│                          │
    │ ← mfa-verify.html           │                          │
    │<──────────────────────────────│                          │
    │                              │                          │
    │ POST /login/mfa              │                          │
    │   { code: "123456" }         │                          │
    │──────────────────────────────>│                          │
    │                              │ TOTP 검증                │
    │                              │──────────────────────────>│
    │                              │<──────────────────────────│
    │                              │                          │
    │                              │ SSO 세션 생성            │
    │                              │ SSO_SESSION 쿠키 설정    │
    │                              │                          │
    │ 302 → /oauth2/authorize      │                          │
    │   (세션 있음 → 인가코드 발급) │                          │
    │<──────────────────────────────│                          │
    │                              │                          │
    │ 302 → redirect_uri?code=...  │                          │
    │<──────────────────────────────│                          │
```

---

## 4. 사용자 수명주기

### 4.1 사용자 상태 모델

```
            ┌──────────┐
            │  PENDING  │ ← 최초 생성 (이메일 미인증)
            └────┬─────┘
                 │ 이메일 인증 / 관리자 활성화
                 ▼
            ┌──────────┐
      ┌─────│  ACTIVE   │─────┐
      │     └────┬─────┘     │
      │          │            │
  비밀번호     관리자        로그인 실패
  만료         비활성화      임계 초과
      │          │            │
      ▼          ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ EXPIRED  │ │ DISABLED │ │  LOCKED  │
└────┬─────┘ └──────────┘ └────┬─────┘
     │                         │
  비밀번호                   잠금 해제
  변경                      (시간 경과/관리자)
     │                         │
     └──────────┐  ┌───────────┘
                ▼  ▼
            ┌──────────┐
            │  ACTIVE   │
            └──────────┘
```

### 4.2 사용자 출처 (V10 마이그레이션)

| 출처 | 설명 | 비밀번호 관리 |
|------|------|-------------|
| `LOCAL` | SSO Server 로컬 DB에 생성된 사용자 | SSO Server가 관리 |
| `LDAP` | LDAP/AD에서 동기화된 사용자 | LDAP 서버가 관리 |

---

## 5. 세션 관리

### 5.1 SSO 세션 구조

```java
SsoSession {
    sessionId: String       // UUID 기반 고유 식별자
    userId: UUID            // 사용자 ID
    username: String        // 사용자명
    ipAddress: String       // 클라이언트 IP
    userAgent: String       // 브라우저 User-Agent
    status: SessionStatus   // ACTIVE, EXPIRED, REVOKED
    createdAt: Instant      // 생성 시각
    lastAccessedAt: Instant // 마지막 접근 시각
    expiresAt: Instant      // 만료 시각
}
```

### 5.2 세션 정책

| 정책 | 기본값 | CC 모드 | SFR |
|------|--------|---------|-----|
| 세션 타임아웃 | 3,600초 (1시간) | 1,800초 (30분) | FTA_SSL.3 |
| 사용자당 최대 세션 | 5 | 3 | FTA_SSL.3 |
| 세션 갱신 | 활동 시 자동 연장 | 활동 시 자동 연장 | - |
| 최대 세션 초과 시 | 가장 오래된 세션 종료 | 가장 오래된 세션 종료 | - |

### 5.3 세션 저장소

현재 `InMemorySessionStore`를 사용하며, `SessionStore` 인터페이스를 통해 추상화되어 있다.

```java
public interface SessionStore {
    void save(SsoSession session);
    Optional<SsoSession> findById(String sessionId);
    List<SsoSession> findByUserId(UUID userId);
    long countByUserId(UUID userId);
    void delete(String sessionId);
    void deleteByUserId(UUID userId);
}
```

- **메모리 기반**: ConcurrentHashMap 사용, 서버 재시작 시 전체 세션 소멸
- **설계 의도**: 서버 재시작 시 강제 재인증으로 보안 강화
- **확장**: Redis 또는 PostgreSQL 기반 구현으로 교체 가능 (HA 환경)

---

## 6. 무차별 대입 보호 (FIA_AFL.1)

### 6.1 구현 메커니즘

`BruteForceProtectionService`는 Caffeine Cache를 사용하여 사용자별 로그인 실패 횟수를 추적한다.

```
로그인 시도
    │
    ▼
isBlocked(username)?
    │
    ├── YES → 401 Account Locked (잠금 상태)
    │
    └── NO → 비밀번호 검증
              │
              ├── 성공 → recordSuccessfulAttempt()
              │          (실패 횟수 초기화, 잠금 해제)
              │
              └── 실패 → recordFailedAttempt()
                          │
                          └── 실패 횟수 >= maxAttempts?
                                  │
                                  ├── YES → lockedCache.put(username, true)
                                  │          (계정 잠금)
                                  │
                                  └── NO → (다음 시도 대기)
```

### 6.2 설정 매개변수

| 매개변수 | 기본값 | CC 모드 | 설명 |
|---------|--------|---------|------|
| `max-login-attempts` | 5 | **3** | 최대 연속 실패 횟수 |
| `lockout-duration` | 1,800초 (30분) | **3,600초** (1시간) | 계정 잠금 지속 시간 |

### 6.3 캐시 구성

- **Caffeine Cache**: `expireAfterWrite(30분)`, `maximumSize(10,000)`
- **두 개의 캐시**: `attemptsCache` (실패 횟수), `lockedCache` (잠금 상태)
- **캐시 만료**: 잠금 기간 경과 후 자동 해제

### 6.4 Rate Limit Filter

`RateLimitFilter`는 모든 API 요청에 대해 IP 기반 요청 빈도를 제한한다.

| 매개변수 | 기본값 | CC 모드 |
|---------|--------|---------|
| `requests-per-second` | 10 | **5** |
| `burst-size` | 20 | **10** |

---

## 7. 비밀번호 정책 (FIA_SOS.1)

### 7.1 `PasswordPolicyService` 정책

| 정책 | 기본값 | CC 모드 | 설명 |
|------|--------|---------|------|
| 최소 길이 | 8자 | **12자** | 비밀번호 최소 문자 수 |
| 최대 길이 | 128자 | 128자 | 비밀번호 최대 문자 수 |
| 이력 수 | 5 | **10** | 최근 N개 비밀번호 재사용 금지 |

### 7.2 비밀번호 이력 관리

`sso_password_history` 테이블에 이전 비밀번호 해시를 저장하여 재사용을 방지한다.

```
비밀번호 변경 요청
    │
    ▼
새 비밀번호 정책 검증 (길이, 복잡도)
    │
    ▼
sso_password_history에서 최근 N개 해시 조회
    │
    ▼
각 이력 해시와 BCrypt 비교
    │
    ├── 일치하는 이력 있음 → 거부 ("이전 비밀번호와 동일")
    │
    └── 일치 없음 → 비밀번호 변경 허용
                      → 새 해시를 이력에 추가
                      → 이력 수 초과 시 가장 오래된 이력 삭제
```

---

## 8. MFA 설정 및 관리 플로우

### 8.1 TOTP 설정 플로우

```
사용자                         MfaController / TotpService
  │                                   │
  │ POST /api/v1/mfa/setup           │
  │──────────────────────────────────>│
  │                                   │
  │                                   │ 1. 20바이트 랜덤 비밀키 생성
  │                                   │ 2. Base32 인코딩
  │                                   │ 3. AES-256-GCM으로 비밀키 암호화
  │                                   │ 4. DB 저장 (encryptedSecret + IV)
  │                                   │ 5. otpauth:// URI 생성
  │                                   │ 6. QR 코드 이미지 생성 (ZXing)
  │                                   │ 7. 복구 코드 10개 생성 (BCrypt 해시 저장)
  │                                   │
  │  200 OK                           │
  │  { secret, qrCodeDataUri,         │
  │    otpauthUri, recoveryCodes[] }   │
  │<──────────────────────────────────│
  │                                   │
  │ (사용자가 인증 앱에 등록)          │
  │                                   │
  │ POST /api/v1/mfa/verify-setup     │
  │  { code: "123456" }               │
  │──────────────────────────────────>│
  │                                   │
  │                                   │ 1. DB에서 암호화된 비밀키 복호화
  │                                   │ 2. TOTP 코드 검증
  │                                   │ 3. verified=true, enabled=true 설정
  │                                   │
  │  200 OK                           │
  │<──────────────────────────────────│
```

### 8.2 MFA 대기 세션

비밀번호 인증 성공 후 MFA 검증까지의 중간 상태를 관리한다.

| 필드 | 설명 |
|------|------|
| `mfaToken` | MFA 대기 세션 식별 토큰 |
| `userId` | 1차 인증 통과한 사용자 ID |
| `createdAt` | 생성 시각 |
| `expiresAt` | 만료 시각 (기본 5분, CC 모드 3분) |

- 만료된 대기 세션으로 MFA 검증 시도 시 401 반환
- MFA 성공 시 대기 세션 소비(consume) 처리

---

## 9. 감사 로그 연동

Identity Engine의 주요 이벤트는 `AuditService`를 통해 감사 로그에 기록된다.

| 이벤트 | 기록 데이터 | 조건 |
|--------|-----------|------|
| 로그인 성공 | userId, ipAddress, userAgent | 항상 |
| 로그인 실패 | username, ipAddress, errorMessage | 항상 |
| 계정 잠금 | username, failedAttempts | 임계 초과 시 |
| MFA 설정 | userId | TOTP 활성화 시 |
| MFA 검증 성공 | userId | MFA 통과 시 |
| MFA 검증 실패 | userId, errorMessage | 코드 불일치 시 |
| 복구 코드 사용 | userId | 복구 코드 사용 시 |
| LDAP 인증 | userId, ldapDN | LDAP 인증 시도 시 |
| 비밀번호 변경 | userId | 성공/실패 모두 |
| 사용자 생성 | resourceId (신규 userId) | 관리자 조치 |

---

## 10. 보안 고려사항

### 10.1 비밀번호 보호

- BCrypt cost=12는 현재 하드웨어에서 약 250ms 소요 (무차별 대입 저항)
- 비밀번호 평문은 로그에 절대 기록하지 않음
- 네트워크 전송 시 TLS 보호 (운영환경 Nginx 책임)

### 10.2 TOTP 보안

- TOTP 비밀키 평문은 DB에 절대 저장하지 않음 (AES-256-GCM 암호화)
- 시간 윈도우 +/-1 설정으로 30초 오차 허용 (총 90초)
- QR 코드는 클라이언트에 1회만 전달, 서버에 저장하지 않음

### 10.3 LDAP 보안

- LDAP Injection 방지 (특수문자 이스케이프)
- 서비스 계정 바인드 비밀번호 환경변수 주입
- LDAPS(TLS) 연결 권장
- 연결/읽기 타임아웃 설정 (5초/10초)

### 10.4 세션 보안

- 세션 ID는 UUID v4 (122비트 랜덤)
- 서버 재시작 시 전체 세션 무효화 (설계 의도)
- SSO_SESSION 쿠키: HttpOnly, Secure, SameSite 설정

---

## 11. 관련 테이블

| 테이블 | 마이그레이션 | 용도 |
|--------|-------------|------|
| `sso_users` | V1, V10 | 사용자 정보 (username, email, passwordHash, source) |
| `sso_password_history` | V2 | 비밀번호 이력 (재사용 방지) |
| `sso_totp_secrets` | V9 | TOTP 비밀키 (AES-256-GCM 암호화) |
| `sso_recovery_codes` | V9 | 복구 코드 (BCrypt 해시, 1회용) |
| `sso_mfa_pending_sessions` | V9 | MFA 대기 세션 |
| `sso_login_attempts` | V7 | 로그인 시도 기록 |

---

## 12. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
