# 운영 가이드 (AGD_OPE)

## AuthFusion SSO Server 운영 지침서

---

## 1. 개요

본 문서는 AuthFusion SSO Server를 CC(Common Criteria) 평가 구성에 부합하도록 운영하기 위한 지침을 제공한다.
관리자는 본 문서의 절차를 따라 TOE를 안전하게 운영해야 한다.

### 1.1 대상 독자

- TOE 운영 관리자
- 보안 관리자
- 시스템 관리자

### 1.2 사전 조건

- AGD_PRE(설치/설정 가이드)에 따라 TOE가 정상 설치되어 있어야 한다
- CC 모드가 활성화되어 있어야 한다
- 관리자 계정이 초기 설정되어 있어야 한다

---

## 2. CC 모드 실행

### 2.1 CC 모드 개요

CC 모드는 TOE의 보안 기능이 CC 평가 구성에 부합하도록 강화된 동작 모드이다.

CC 모드에서는 다음 보안 정책이 강제 적용된다:

| 항목 | CC 모드 설정 | 일반 모드 |
|------|-------------|----------|
| HTTPS 강제 | 활성화 | 선택 |
| 비밀번호 복잡도 | 12자 이상, 특수문자 필수 | 8자 이상 |
| 세션 타임아웃 | 30분 (비활성) | 60분 |
| 계정 잠금 | 5회 실패 시 30분 잠금 | 10회 실패 시 15분 잠금 |
| 감사 로그 | 전체 이벤트 기록 | 주요 이벤트만 기록 |
| 키 길이 최소값 | RSA-2048 | RSA-2048 |
| 디버그 로그 | 비활성화 | 선택 |

### 2.2 CC 모드 활성화

`application-cc.yml` 프로파일을 활성화하여 CC 모드로 실행한다:

```bash
java -jar authfusion-sso-server.jar --spring.profiles.active=cc
```

### 2.3 CC 모드 환경 변수

```bash
# CC 모드 필수 환경 변수
export AUTHFUSION_CC_MODE=true
export AUTHFUSION_HTTPS_REQUIRED=true
export AUTHFUSION_MASTER_KEY_SOURCE=vault   # vault 또는 hsm
export AUTHFUSION_AUDIT_LEVEL=FULL
export AUTHFUSION_SESSION_TIMEOUT=1800      # 30분 (초)
export AUTHFUSION_LOCKOUT_THRESHOLD=5
export AUTHFUSION_LOCKOUT_DURATION=1800     # 30분 (초)
```

### 2.4 CC 모드 확인

CC 모드 활성화 상태를 확인하는 방법:

```bash
curl -s https://sso.aines.kr/actuator/health | jq '.components.ccMode'
```

응답 예시:
```json
{
  "status": "UP",
  "details": {
    "ccModeEnabled": true,
    "httpsEnforced": true,
    "auditLevel": "FULL"
  }
}
```

---

## 3. 보안 파라미터 관리

### 3.1 인증 파라미터

#### 3.1.1 비밀번호 정책

| 파라미터 | CC 모드 기본값 | 설명 |
|---------|-------------|------|
| `authfusion.password.min-length` | 12 | 최소 비밀번호 길이 |
| `authfusion.password.require-uppercase` | true | 대문자 필수 |
| `authfusion.password.require-lowercase` | true | 소문자 필수 |
| `authfusion.password.require-digit` | true | 숫자 필수 |
| `authfusion.password.require-special` | true | 특수문자 필수 |
| `authfusion.password.history-count` | 5 | 비밀번호 이력 저장 수 |
| `authfusion.password.max-age-days` | 90 | 비밀번호 최대 사용 기간 (일) |

#### 3.1.2 계정 잠금 정책

| 파라미터 | CC 모드 기본값 | 설명 |
|---------|-------------|------|
| `authfusion.lockout.threshold` | 5 | 잠금까지 허용 실패 횟수 |
| `authfusion.lockout.duration` | 1800 | 잠금 지속 시간 (초) |
| `authfusion.lockout.reset-after` | 3600 | 실패 카운터 초기화 시간 (초) |

### 3.2 토큰 파라미터

| 파라미터 | CC 모드 기본값 | 설명 |
|---------|-------------|------|
| `authfusion.token.access-token-ttl` | 900 | Access Token 유효 시간 (초, 15분) |
| `authfusion.token.refresh-token-ttl` | 3600 | Refresh Token 유효 시간 (초, 1시간) |
| `authfusion.token.id-token-ttl` | 900 | ID Token 유효 시간 (초, 15분) |
| `authfusion.token.authorization-code-ttl` | 300 | Authorization Code 유효 시간 (초, 5분) |

### 3.3 세션 파라미터

| 파라미터 | CC 모드 기본값 | 설명 |
|---------|-------------|------|
| `authfusion.session.timeout` | 1800 | 세션 비활성 타임아웃 (초, 30분) |
| `authfusion.session.max-concurrent` | 3 | 사용자당 최대 동시 세션 수 |
| `authfusion.session.absolute-timeout` | 28800 | 세션 절대 타임아웃 (초, 8시간) |

### 3.4 암호 파라미터

| 파라미터 | CC 모드 기본값 | 설명 |
|---------|-------------|------|
| `authfusion.crypto.key-algorithm` | RSA | 서명 키 알고리즘 |
| `authfusion.crypto.key-size` | 2048 | RSA 키 크기 (비트) |
| `authfusion.crypto.signing-algorithm` | RS256 | JWT 서명 알고리즘 |
| `authfusion.crypto.key-encryption` | AES-256-GCM | 키 암호화 알고리즘 |
| `authfusion.crypto.bcrypt-strength` | 12 | BCrypt cost factor |

---

## 4. 감사 로그 관리

### 4.1 감사 이벤트 유형

CC 모드에서는 다음 모든 이벤트가 기록된다:

| 이벤트 유형 | 설명 | SFR |
|------------|------|-----|
| `AUTH_LOGIN_SUCCESS` | 로그인 성공 | FAU_GEN.1 |
| `AUTH_LOGIN_FAILURE` | 로그인 실패 | FAU_GEN.1 |
| `AUTH_LOGOUT` | 로그아웃 | FAU_GEN.1 |
| `AUTH_ACCOUNT_LOCKED` | 계정 잠금 | FAU_GEN.1, FIA_AFL.1 |
| `TOKEN_ISSUED` | 토큰 발급 | FAU_GEN.1 |
| `TOKEN_REFRESHED` | 토큰 갱신 | FAU_GEN.1 |
| `TOKEN_REVOKED` | 토큰 폐기 | FAU_GEN.1 |
| `TOKEN_VALIDATION_FAILURE` | 토큰 검증 실패 | FAU_GEN.1 |
| `SESSION_CREATED` | 세션 생성 | FAU_GEN.1 |
| `SESSION_EXPIRED` | 세션 만료 | FAU_GEN.1 |
| `SESSION_TERMINATED` | 세션 강제 종료 | FAU_GEN.1 |
| `KEY_GENERATED` | 서명키 생성 | FAU_GEN.1, FCS_CKM.1 |
| `KEY_ROTATED` | 서명키 로테이션 | FAU_GEN.1 |
| `KEY_DESTROYED` | 서명키 파기 | FAU_GEN.1, FCS_CKM.4 |
| `USER_CREATED` | 사용자 생성 | FAU_GEN.1 |
| `USER_MODIFIED` | 사용자 수정 | FAU_GEN.1 |
| `USER_DELETED` | 사용자 삭제 | FAU_GEN.1 |
| `CLIENT_CREATED` | 클라이언트 등록 | FAU_GEN.1 |
| `CLIENT_MODIFIED` | 클라이언트 수정 | FAU_GEN.1 |
| `CLIENT_DELETED` | 클라이언트 삭제 | FAU_GEN.1 |
| `ROLE_ASSIGNED` | 역할 할당 | FAU_GEN.1 |
| `ROLE_REVOKED` | 역할 회수 | FAU_GEN.1 |
| `CONFIG_CHANGED` | 설정 변경 | FAU_GEN.1 |
| `RATE_LIMIT_EXCEEDED` | 요청 빈도 제한 초과 | FAU_GEN.1 |

### 4.2 감사 로그 조회

관리자는 REST API를 통해 감사 로그를 조회할 수 있다:

```bash
# 전체 감사 이벤트 조회 (페이징)
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/audit/events?page=0&size=50"

# 이벤트 유형별 조회
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/audit/events?eventType=AUTH_LOGIN_FAILURE"

# 날짜 범위 조회
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/audit/events?from=2026-03-01&to=2026-03-03"

# 사용자별 조회
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/audit/events?username=admin"
```

### 4.3 감사 로그 보존

- **기본 보존 기간**: 90일 (DB 내)
- **장기 보존**: SIEM 연동을 통한 외부 보관 권장
- **삭제 금지**: CC 모드에서는 감사 로그 수동 삭제가 비활성화됨

### 4.4 감사 로그 무결성

감사 로그의 무결성을 보장하기 위해 다음 메커니즘이 적용된다:

- 감사 레코드에 HMAC 서명 포함
- DB 레벨 감사 테이블 트리거 (UPDATE/DELETE 방지)
- 외부 SIEM 실시간 전송 (선택)

---

## 5. 키 관리 및 로테이션

### 5.1 서명키 관리

#### 5.1.1 키 페어 구조

```
┌─────────────────────────────────────┐
│  마스터 키 (Master Key)              │
│  소스: Vault / HSM / 환경변수        │
│  용도: 서명키 암호화                  │
├─────────────────────────────────────┤
│  서명키 (Signing Key)                │
│  알고리즘: RSA-2048+                 │
│  용도: JWT 서명/검증                  │
│  저장: DB (AES-256-GCM 암호화)       │
└─────────────────────────────────────┘
```

#### 5.1.2 키 로테이션 절차

1. 새 RSA 키 페어 생성
2. 새 키를 마스터 키로 암호화하여 DB에 저장
3. 새 키를 활성(Active) 상태로 전환
4. 이전 키를 비활성(Inactive) 상태로 변경 (유예 기간 동안 검증용으로 유지)
5. 유예 기간 경과 후 이전 키를 안전 삭제

```bash
# 수동 키 로테이션 (관리 API)
curl -X POST -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/keys/rotate"
```

#### 5.1.3 키 로테이션 주기

| 환경 | 권장 로테이션 주기 |
|------|------------------|
| CC 모드 (운영) | 90일 |
| CC 모드 (고보안) | 30일 |
| 일반 모드 | 180일 |

### 5.2 마스터 키 관리

마스터 키는 서명키를 암호화하는 데 사용되며, TOE 외부에서 관리된다:

| 소스 | 설정 | 보안 수준 |
|------|------|----------|
| HashiCorp Vault | `AUTHFUSION_MASTER_KEY_SOURCE=vault` | 권장 |
| Thales Luna HSM | `AUTHFUSION_MASTER_KEY_SOURCE=hsm` | 최고 |
| 환경 변수 | `AUTHFUSION_MASTER_KEY_SOURCE=env` | CC 모드 비권장 |

---

## 6. 세션 관리

### 6.1 세션 생명주기

```
세션 생성 → 활성 → [비활성 타임아웃 / 절대 타임아웃 / 강제 종료] → 종료
```

### 6.2 세션 모니터링

```bash
# 활성 세션 목록 조회
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/sessions?status=ACTIVE"

# 특정 사용자의 세션 조회
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/sessions?username=user01"
```

### 6.3 세션 강제 종료

보안 위협이 감지된 경우 관리자가 세션을 강제 종료할 수 있다:

```bash
# 특정 세션 강제 종료
curl -X DELETE -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/sessions/{sessionId}"

# 특정 사용자의 전체 세션 종료
curl -X DELETE -H "Authorization: Bearer <admin-token>" \
  "https://sso.aines.kr/api/v1/sessions?username=user01"
```

### 6.4 세션 관련 감사

세션 관련 모든 이벤트(생성, 만료, 강제 종료)는 감사 로그에 자동 기록된다.

---

## 7. 일상 운영 절차

### 7.1 일일 점검 항목

| 항목 | 점검 방법 | 빈도 |
|------|----------|------|
| 서비스 상태 확인 | `/actuator/health` 엔드포인트 조회 | 매일 |
| 인증 실패 로그 검토 | 감사 로그에서 `AUTH_LOGIN_FAILURE` 조회 | 매일 |
| 계정 잠금 현황 확인 | 감사 로그에서 `AUTH_ACCOUNT_LOCKED` 조회 | 매일 |
| 비정상 세션 확인 | 활성 세션 목록 검토 | 매일 |

### 7.2 주간 점검 항목

| 항목 | 점검 방법 | 빈도 |
|------|----------|------|
| 감사 로그 용량 확인 | DB 테이블 크기 확인 | 주 1회 |
| 토큰 발급 통계 검토 | 감사 통계 API 조회 | 주 1회 |
| 비정상 접근 패턴 분석 | SIEM 대시보드 검토 | 주 1회 |

### 7.3 월간 점검 항목

| 항목 | 점검 방법 | 빈도 |
|------|----------|------|
| 키 로테이션 상태 확인 | 서명키 목록 및 만료일 확인 | 월 1회 |
| 사용자/클라이언트 정리 | 미사용 계정/클라이언트 비활성화 | 월 1회 |
| 보안 패치 적용 | TOE 업데이트 확인 및 적용 | 월 1회 |
| 설정 검토 | CC 모드 파라미터 준수 여부 확인 | 월 1회 |

---

## 8. 장애 대응

### 8.1 서비스 장애

- TOE 프로세스가 비정상 종료된 경우, 재시작 전 감사 로그를 보존한다
- 재시작 시 반드시 CC 모드 프로파일로 실행한다

### 8.2 보안 사고 대응

1. 영향받는 세션 즉시 강제 종료
2. 관련 계정 잠금
3. 감사 로그 수집 및 보존
4. 필요 시 서명키 긴급 로테이션
5. 사고 보고서 작성

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
