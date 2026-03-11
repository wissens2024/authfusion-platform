# 감사 로깅 아키텍처 (Audit Logging Architecture)

## AuthFusion SSO Server v1.0

---

## 1. 개요

감사 로깅 서브시스템은 CC(Common Criteria) 인증의 FAU(Security Audit) 요구사항을 충족하기 위한 핵심 기능이다.
SSO Server에서 발생하는 모든 보안 관련 이벤트를 구조화된 형식으로 기록하고, 조회 및 분석 기능을 제공한다.

### 1.1 관련 SFR

| SFR | 명칭 | 감사 서브시스템 구현 |
|-----|------|-------------------|
| FAU_GEN.1 | 감사 데이터 생성 | 모든 보안 이벤트 자동 기록 |
| FAU_GEN.2 | 사용자 식별 연관 | 이벤트에 userId, ipAddress, clientId, userAgent 포함 |

### 1.2 설계 원칙

1. **완전성**: 모든 보안 관련 이벤트를 빠짐없이 기록
2. **정확성**: 이벤트 발생 시각, 주체, 결과를 정확히 기록
3. **무결성**: 기록된 감사 로그의 무단 수정/삭제 방지
4. **가용성**: 감사 로그 조회를 위한 다양한 필터링 기능 제공
5. **지속성**: PostgreSQL에 영구 저장하여 서버 재시작에도 유지

### 1.3 구성 모듈

```
audit/
├── model/
│   ├── AuditEventEntity.java          # 감사 이벤트 JPA 엔티티 (@ToeScope)
│   ├── AuditEventType.java            # 이벤트 유형 열거형
│   ├── AuditEventResponse.java        # 조회 응답 DTO
│   └── AuditStatisticsResponse.java   # 통계 응답 DTO
├── repository/
│   └── AuditEventRepository.java      # JPA 리포지토리 (@ToeScope)
├── service/
│   └── AuditService.java              # 감사 서비스 (@ToeScope)
└── controller/
    ├── AuditController.java           # 감사 조회 API (@ToeScope)
    └── AuditStatisticsController.java # 감사 통계 API (@ExtendedFeature)
```

---

## 2. 감사 이벤트 유형

### 2.1 이벤트 타입 전체 목록

`AuditEventType` 열거형에 정의된 10개의 이벤트 유형:

| 이벤트 타입 | 분류 | 설명 | 관련 SFR |
|------------|------|------|---------|
| `AUTHENTICATION` | 인증 | 사용자 로그인 성공/실패, 계정 잠금 | FIA_UAU.1, FIA_AFL.1 |
| `AUTHORIZATION` | 인가 | OAuth2 인가 코드 발급, 접근 제어 결정 | FIA_UAU.1, FDP_ACC.1 |
| `USER_MANAGEMENT` | 사용자 | 사용자 생성, 수정, 비활성화, 비밀번호 변경 | FIA_SOS.1 |
| `CLIENT_MANAGEMENT` | 클라이언트 | OAuth2 클라이언트 등록, 수정, 삭제 | - |
| `ROLE_MANAGEMENT` | 역할 | RBAC 역할 생성, 수정, 사용자-역할 매핑 | FDP_ACC.1 |
| `SESSION_MANAGEMENT` | 세션 | 세션 생성, 만료, 강제 종료 | FTA_SSL.3 |
| `TOKEN_OPERATION` | 토큰 | 토큰 발급, 갱신, 폐기 | FCS_COP.1 |
| `MFA_OPERATION` | MFA | TOTP 설정, 검증, 비활성화, 복구 코드 사용 | FIA_UAU.1 |
| `LDAP_OPERATION` | LDAP | LDAP 인증 시도, 사용자 동기화 | FIA_UAU.1 |
| `SYSTEM` | 시스템 | 서버 기동/종료, 키 로테이션, 설정 변경 | FCS_CKM.1 |

### 2.2 이벤트별 action 상세

#### AUTHENTICATION 이벤트

| Action | 설명 | 기록 조건 |
|--------|------|----------|
| `LOGIN_SUCCESS` | 로그인 성공 | 비밀번호 인증 성공 시 |
| `LOGIN_FAILED` | 로그인 실패 | 비밀번호 불일치, 사용자 없음 |
| `LOGIN_LOCKED` | 계정 잠금으로 거부 | 잠금 상태에서 로그인 시도 |
| `LOGOUT` | 로그아웃 | 명시적 로그아웃 |
| `PASSWORD_CHANGE` | 비밀번호 변경 | 성공/실패 모두 |

#### TOKEN_OPERATION 이벤트

| Action | 설명 | 기록 조건 |
|--------|------|----------|
| `TOKEN_ISSUED` | 토큰 발급 | Access/ID/Refresh Token 발급 시 |
| `TOKEN_REFRESHED` | 토큰 갱신 | Refresh Token으로 새 토큰 발급 |
| `TOKEN_REVOKED` | 토큰 폐기 | 명시적 폐기 요청 |
| `CODE_ISSUED` | 인가 코드 발급 | Authorization Code 발급 시 |

#### MFA_OPERATION 이벤트

| Action | 설명 | 기록 조건 |
|--------|------|----------|
| `TOTP_SETUP` | TOTP 설정 시작 | 비밀키 생성 시 |
| `TOTP_VERIFIED` | TOTP 설정 검증 | 최초 코드 검증 성공 |
| `TOTP_VERIFY_SUCCESS` | TOTP 인증 성공 | 로그인 시 MFA 통과 |
| `TOTP_VERIFY_FAILED` | TOTP 인증 실패 | 잘못된 코드 입력 |
| `TOTP_DISABLED` | TOTP 비활성화 | 사용자 또는 관리자 조치 |
| `RECOVERY_CODE_USED` | 복구 코드 사용 | TOTP 대신 복구 코드 인증 |
| `RECOVERY_CODE_REGENERATED` | 복구 코드 재생성 | 새 복구 코드 발급 |

#### SYSTEM 이벤트

| Action | 설명 | 기록 조건 |
|--------|------|----------|
| `KEY_ROTATED` | 서명키 로테이션 | 새 RSA 키 페어 생성 시 |
| `SERVER_STARTED` | 서버 기동 | 애플리케이션 시작 시 |
| `CONFIG_CHANGED` | 설정 변경 | 런타임 설정 변경 시 |

---

## 3. 감사 이벤트 데이터 구조

### 3.1 AuditEventEntity

`sso_audit_events` 테이블에 저장되는 감사 이벤트 엔티티이다.

```java
@ToeScope(value = "감사 이벤트 엔티티", sfr = {"FAU_GEN.1"})
@Entity
@Table(name = "sso_audit_events")
public class AuditEventEntity {

    UUID id;                  // PK (UUID 자동 생성)
    String eventType;         // 이벤트 유형 (AUTHENTICATION, TOKEN_OPERATION 등)
    String action;            // 구체적 액션 (LOGIN_SUCCESS, TOKEN_ISSUED 등)
    String userId;            // 이벤트 주체 사용자 ID (nullable)
    String clientId;          // 관련 OAuth2 클라이언트 ID (nullable)
    String ipAddress;         // 클라이언트 IP 주소 (최대 45자, IPv6 지원)
    String userAgent;         // 브라우저 User-Agent (최대 512자)
    String resourceType;      // 대상 리소스 유형 (USER, CLIENT, ROLE 등)
    String resourceId;        // 대상 리소스 ID
    boolean success;          // 이벤트 성공/실패 여부
    String errorMessage;      // 실패 시 오류 메시지 (최대 1024자)
    String details;           // 추가 상세 정보 (TEXT, JSON 형식 가능)
    LocalDateTime timestamp;  // 이벤트 발생 시각 (UTC)
}
```

### 3.2 데이터베이스 인덱스

성능을 위해 다음 인덱스가 생성되어 있다:

| 인덱스 | 컬럼 | 용도 |
|--------|------|------|
| `idx_sso_audit_event_type` | `event_type` | 이벤트 유형별 조회 |
| `idx_sso_audit_action` | `action` | 액션별 조회 |
| `idx_sso_audit_user_id` | `user_id` | 사용자별 조회 |
| `idx_sso_audit_timestamp` | `timestamp` | 시간 범위 조회 (정렬) |

### 3.3 FAU_GEN.2 필수 필드

CC FAU_GEN.2 요구사항에 따라 각 감사 이벤트는 다음 정보를 반드시 포함한다:

| 필드 | CC 요구사항 | 구현 |
|------|-----------|------|
| 이벤트 유형 | 이벤트의 종류 | `eventType` + `action` |
| 발생 시각 | 이벤트 발생 시각 | `timestamp` (UTC, @PrePersist 자동 설정) |
| 주체 식별 | 이벤트를 유발한 사용자 | `userId` |
| 성공/실패 | 이벤트 결과 | `success` (boolean) |
| 출발지 정보 | 이벤트 출발지 | `ipAddress`, `userAgent` |
| 추가 정보 | 이벤트별 상세 | `details`, `errorMessage` |

---

## 4. AuditService API

### 4.1 이벤트 기록 메소드

`AuditService`는 다음의 편의 메소드를 제공한다:

```java
@ToeScope(value = "감사 로그 서비스", sfr = {"FAU_GEN.1", "FAU_GEN.2"})
@Service
public class AuditService {

    // 범용 이벤트 기록
    AuditEventEntity logEvent(AuditEventEntity event);

    // 인증 이벤트 기록
    void logAuthentication(String action, String userId,
                           String ipAddress, boolean success,
                           String errorMessage);

    // 사용자 관리 이벤트 기록
    void logUserManagement(String action, String userId,
                            String resourceId, boolean success,
                            String errorMessage);

    // 클라이언트 관리 이벤트 기록
    void logClientManagement(String action, String userId,
                              String clientId, boolean success,
                              String errorMessage);

    // 토큰 연산 이벤트 기록
    void logTokenOperation(String action, String userId,
                            String clientId, boolean success,
                            String errorMessage);

    // 감사 이벤트 조회 (페이징 + 필터)
    Page<AuditEventResponse> getEvents(String eventType, String userId,
                                        String action, Boolean success,
                                        LocalDateTime from, LocalDateTime to,
                                        int page, int size);

    // 감사 통계 조회 (@ExtendedFeature)
    AuditStatisticsResponse getStatistics();
}
```

### 4.2 이벤트 기록 시점

각 보안 기능 모듈에서 `AuditService`를 주입받아 적절한 시점에 감사 이벤트를 기록한다:

```
[AuthController]
  ├─ login() 성공 → logAuthentication("LOGIN_SUCCESS", ...)
  ├─ login() 실패 → logAuthentication("LOGIN_FAILED", ...)
  └─ logout()     → logAuthentication("LOGOUT", ...)

[TokenEndpoint]
  ├─ 토큰 발급   → logTokenOperation("TOKEN_ISSUED", ...)
  └─ 토큰 갱신   → logTokenOperation("TOKEN_REFRESHED", ...)

[RevocationEndpoint]
  └─ 토큰 폐기   → logTokenOperation("TOKEN_REVOKED", ...)

[AuthorizationEndpoint]
  └─ 코드 발급   → logTokenOperation("CODE_ISSUED", ...)

[MfaController / TotpService]
  ├─ TOTP 설정   → logEvent(MFA_OPERATION, "TOTP_SETUP", ...)
  ├─ TOTP 검증   → logEvent(MFA_OPERATION, "TOTP_VERIFY_*", ...)
  └─ 복구코드    → logEvent(MFA_OPERATION, "RECOVERY_CODE_USED", ...)

[KeyPairManager]
  └─ 키 로테이션 → logEvent(SYSTEM, "KEY_ROTATED", ...)

[BruteForceProtectionService]
  └─ 계정 잠금   → logAuthentication("LOGIN_LOCKED", ...)
```

---

## 5. 감사 로그 조회 API

### 5.1 이벤트 조회 (TOE)

```
GET /api/v1/audit/events
```

**쿼리 매개변수:**

| 매개변수 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `eventType` | String | 아니오 | 이벤트 유형 필터 (AUTHENTICATION 등) |
| `userId` | String | 아니오 | 사용자 ID 필터 |
| `action` | String | 아니오 | 액션 필터 (LOGIN_SUCCESS 등) |
| `success` | Boolean | 아니오 | 성공/실패 필터 |
| `from` | ISO DateTime | 아니오 | 시작 시각 |
| `to` | ISO DateTime | 아니오 | 종료 시각 |
| `page` | Integer | 아니오 | 페이지 번호 (0부터) |
| `size` | Integer | 아니오 | 페이지 크기 (기본 20) |

**응답 예시:**

```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "eventType": "AUTHENTICATION",
      "action": "LOGIN_SUCCESS",
      "userId": "user-uuid-here",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0...",
      "success": true,
      "timestamp": "2026-03-04T10:30:00Z"
    }
  ],
  "totalElements": 1500,
  "totalPages": 75,
  "number": 0,
  "size": 20
}
```

### 5.2 감사 통계 (@ExtendedFeature)

```
GET /api/v1/audit/statistics
```

CC 모드에서는 비활성화된다. 기본 모드에서만 사용 가능하다.

**응답:**

```json
{
  "totalEvents": 15000,
  "successfulEvents": 14200,
  "failedEvents": 800,
  "eventsByType": {
    "AUTHENTICATION": 8000,
    "TOKEN_OPERATION": 5000,
    "MFA_OPERATION": 1200,
    "SESSION_MANAGEMENT": 500,
    "USER_MANAGEMENT": 200,
    "SYSTEM": 100
  },
  "eventsByAction": {
    "LOGIN_SUCCESS": 6500,
    "TOKEN_ISSUED": 5000,
    "LOGIN_FAILED": 1500,
    "TOTP_VERIFY_SUCCESS": 1000
  }
}
```

---

## 6. 감사 로그 저장 및 보존

### 6.1 저장소

- **주 저장소**: PostgreSQL `sso_audit_events` 테이블
- **트랜잭션**: 감사 이벤트는 `@Transactional`로 원자적 저장
- **시간대**: UTC 기준 저장 (`LocalDateTime`, JPA `time_zone: UTC`)

### 6.2 보존 정책

TOE 자체에는 감사 로그 삭제 기능이 포함되어 있지 않다.
감사 로그의 장기 보존과 정리는 운영환경의 책임이다.

**권장 보존 구성:**

| 보존 기간 | 저장소 | 비고 |
|-----------|--------|------|
| 0~90일 | PostgreSQL (온라인) | 실시간 조회 가능 |
| 90일~1년 | SIEM 또는 아카이브 | 검색 가능 장기 보관 |
| 1년 이상 | 콜드 스토리지 | 규정 준수용 장기 보관 |

**운영 권장사항:**

```sql
-- 90일 이전 감사 로그를 아카이브 테이블로 이동 (예시)
INSERT INTO sso_audit_events_archive
    SELECT * FROM sso_audit_events
    WHERE timestamp < NOW() - INTERVAL '90 days';

DELETE FROM sso_audit_events
    WHERE timestamp < NOW() - INTERVAL '90 days';
```

### 6.3 무결성 보장

| 보호 수단 | 구현 위치 | 설명 |
|-----------|----------|------|
| DB 레벨 접근 제어 | PostgreSQL | 감사 테이블에 대한 DELETE/UPDATE 권한 제한 |
| 애플리케이션 레벨 | AuditService | 감사 로그 삭제 API 미제공 (TOE) |
| 네트워크 레벨 | PostgreSQL pg_hba.conf | DB 접근 IP 제한 |
| SIEM 전송 | 운영환경 | 별도 저장소에 실시간 복제 |

---

## 7. 로그 파일 감사

PostgreSQL 감사 테이블 외에, 애플리케이션 로그 파일에도 감사 관련 정보가 기록된다.

### 7.1 로그 설정

```yaml
# 기본 모드
logging:
  level:
    com.authfusion.sso.audit: DEBUG
  file:
    name: logs/sso-server.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

# CC 모드 (하드닝)
logging:
  level:
    com.authfusion.sso.audit: DEBUG    # 감사 로그는 DEBUG 유지
    com.authfusion.sso: INFO           # 일반 로그는 INFO로 상향
```

### 7.2 로그 출력 예시

```
2026-03-04 10:30:00.123 [http-nio-8080-exec-1] DEBUG AuditService - Audit event logged: AUTHENTICATION - LOGIN_SUCCESS (success: true)
2026-03-04 10:30:05.456 [http-nio-8080-exec-2] DEBUG AuditService - Audit event logged: TOKEN_OPERATION - TOKEN_ISSUED (success: true)
2026-03-04 10:31:00.789 [http-nio-8080-exec-3] DEBUG AuditService - Audit event logged: AUTHENTICATION - LOGIN_FAILED (success: false)
```

---

## 8. SIEM 연동 가이드

### 8.1 연동 방식

감사 로그를 외부 SIEM으로 전송하는 권장 방식:

| 방식 | 설명 | 장점 |
|------|------|------|
| DB 직접 조회 | SIEM이 PostgreSQL을 주기적으로 폴링 | 구현 단순 |
| 로그 파일 수집 | Filebeat/Fluentd로 로그 파일 전송 | 실시간성 |
| Syslog 전송 | Logback Syslog Appender 추가 | 표준 프로토콜 |
| API 조회 | SIEM이 `/api/v1/audit/events` 폴링 | REST 표준 |

### 8.2 Syslog 연동 예시 (Logback)

```xml
<appender name="SYSLOG" class="ch.qos.logback.classic.net.SyslogAppender">
    <syslogHost>siem-server.example.com</syslogHost>
    <port>514</port>
    <facility>AUTH</facility>
    <suffixPattern>%d{ISO8601} %logger{36} - %msg%n</suffixPattern>
</appender>
```

---

## 9. CC 요구사항 매핑 상세

| CC 요구사항 | 요구 내용 | 구현 방법 |
|------------|----------|----------|
| FAU_GEN.1 (a) | 감사 기능의 기동/종료 기록 | SYSTEM 이벤트 (SERVER_STARTED) |
| FAU_GEN.1 (b) | 각 감사 이벤트의 날짜/시간, 이벤트 유형, 주체 식별, 성공/실패 | AuditEventEntity 필드 |
| FAU_GEN.1 (c) | 추가 정보 기록 | details 필드 (JSON 형식) |
| FAU_GEN.2 | 감사 이벤트를 해당 사용자와 연관 | userId, ipAddress, userAgent 필드 |

### 9.1 감사 대상 이벤트와 SFR 매핑

| SFR 이벤트 | 감사 이벤트 | Action |
|-----------|-----------|--------|
| FIA_UAU.1 - 인증 시도 | AUTHENTICATION | LOGIN_SUCCESS, LOGIN_FAILED |
| FIA_UID.1 - 식별 시도 | AUTHENTICATION | LOGIN_SUCCESS, LOGIN_FAILED |
| FIA_AFL.1 - 인증 실패 임계 도달 | AUTHENTICATION | LOGIN_LOCKED |
| FIA_SOS.1 - 비밀번호 변경 | AUTHENTICATION | PASSWORD_CHANGE |
| FCS_COP.1 - 암호 연산 수행 | TOKEN_OPERATION | TOKEN_ISSUED |
| FCS_CKM.1 - 키 생성 | SYSTEM | KEY_ROTATED |
| FDP_ACC.1 - 접근 제어 결정 | AUTHORIZATION | AUTHORIZE_SUCCESS, AUTHORIZE_DENIED |
| FTA_SSL.3 - 세션 종료 | SESSION_MANAGEMENT | SESSION_EXPIRED, SESSION_REVOKED |

---

## 10. 성능 고려사항

### 10.1 감사 로그 쓰기 성능

- 감사 이벤트는 비동기 처리를 고려할 수 있으나, CC 요구사항 상 이벤트 손실 방지를 위해 동기 쓰기 사용
- `@Transactional`로 DB 원자성 보장
- 인덱스 4개로 조회 성능 최적화

### 10.2 대량 데이터 관리

- 일일 예상 이벤트 수: 10,000~100,000건 (사용자 규모에 따라)
- 90일 보존 시 약 300만~900만 건
- PostgreSQL 파티셔닝 권장 (월별 또는 분기별)

```sql
-- 테이블 파티셔닝 예시 (운영환경 설정)
CREATE TABLE sso_audit_events (
    ...
) PARTITION BY RANGE (timestamp);

CREATE TABLE sso_audit_events_2026_q1
    PARTITION OF sso_audit_events
    FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
```

---

## 11. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
