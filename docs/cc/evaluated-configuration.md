# 평가 구성 정의서 (Evaluated Configuration)

## AuthFusion SSO Server v1.0

---

## 1. 문서 개요

### 1.1 목적

본 문서는 AuthFusion SSO Server의 CC(Common Criteria) 평가 구성(Evaluated Configuration)을 정의한다.
평가 구성이란 CC 인증을 받은 정확한 설정과 배포 형태를 의미하며, 이 구성에서 벗어나면 CC 인증이 적용되지 않는다.
운영자는 반드시 본 문서에 명시된 설정을 준수하여 배포해야 한다.

### 1.2 적용 범위

- **제품명**: AuthFusion SSO Server v1.0.0
- **평가 구성 코드**: AF-SSO-CC-v1.0
- **CC 버전**: Common Criteria v3.1 Revision 5
- **보증 등급**: EAL2+ (증강: ALC_FLR.1)

---

## 2. 평가 대상 소프트웨어 버전

### 2.1 TOE 소프트웨어

| 구성요소 | 버전 | 아티팩트 | 비고 |
|----------|------|---------|------|
| SSO Server | 1.0.0 | `authfusion-sso-server-1.0.0.jar` | Spring Boot 실행 가능 JAR |
| SSO Agent | 1.0.0 | `authfusion-sso-agent-1.0.0.jar` | Servlet Filter 라이브러리 JAR |

### 2.2 필수 런타임 의존성

| 구성요소 | 최소 버전 | 권장 버전 | 비고 |
|----------|----------|----------|------|
| Java Runtime | 17.0.0 | 17.0.10+ (LTS) | OpenJDK 또는 Oracle JDK |
| PostgreSQL | 16.0 | 16.4+ | 주 데이터베이스 |
| Operating System | - | RHEL 8+, Ubuntu 22.04+ | Linux 64-bit |

### 2.3 선택적 운영환경 구성요소

| 구성요소 | 버전 | 용도 | 필수 여부 |
|----------|------|------|----------|
| HashiCorp Vault | 1.17+ | 시크릿 관리 | 권장 |
| Thales Luna HSM | 7.x+ | 하드웨어 키 저장 | 선택 |
| LDAP/AD | LDAPv3 호환 | 외부 사용자 저장소 | 선택 |
| Nginx | 1.24+ | TLS Reverse Proxy | 권장 |
| NTP Server | Stratum 2 이하 | 시간 동기화 | 필수 |

---

## 3. CC 프로파일 설정 (application-cc.yml)

CC 평가 구성에서는 반드시 `cc` Spring 프로파일을 활성화해야 한다.
아래는 `application-cc.yml`의 전체 설정이며, 각 항목의 의미와 기본값 대비 변경 사항을 설명한다.

### 3.1 핵심 CC 설정

```yaml
authfusion:
  sso:
    cc:
      mode: minimum                    # CC 최소 TOE 모드
      extended-features-enabled: false  # [필수] 확장 기능 비활성화
```

| 설정 키 | CC 모드 값 | 기본값 | 설명 |
|---------|-----------|--------|------|
| `authfusion.sso.cc.mode` | `minimum` | (미설정) | CC 최소 TOE 모드 표시 |
| `authfusion.sso.cc.extended-features-enabled` | **`false`** | `true` | 확장 컨트롤러/기능 비활성화. **CC 인증의 가장 핵심적인 설정** |

### 3.2 JWT/토큰 하드닝

```yaml
authfusion:
  sso:
    jwt:
      access-token-validity: 300       # 5분 (변경 없음)
      refresh-token-validity: 43200    # 12시간 (기본 24시간에서 단축)
      id-token-validity: 3600          # 1시간 (변경 없음)
      key-rotation-days: 90            # 90일 (변경 없음)
      key-size: 2048                   # RSA 2048-bit (변경 없음)
```

| 설정 키 | CC 모드 값 | 기본값 | 변경 사유 |
|---------|-----------|--------|----------|
| `jwt.access-token-validity` | 300초 | 300초 | 유지 - 이미 충분히 짧음 |
| `jwt.refresh-token-validity` | **43,200초** | 86,400초 | 하드닝 - 12시간으로 단축 |
| `jwt.id-token-validity` | 3,600초 | 3,600초 | 유지 |
| `jwt.key-rotation-days` | 90일 | 90일 | 유지 - NIST 권고 준수 |
| `jwt.key-size` | 2048 | 2048 | 유지 - RSA 2048-bit 최소 |

### 3.3 MFA 하드닝

```yaml
authfusion:
  sso:
    mfa:
      pending-session:
        timeout: 180                   # 3분 (기본 5분에서 단축)
```

| 설정 키 | CC 모드 값 | 기본값 | 변경 사유 |
|---------|-----------|--------|----------|
| `mfa.pending-session.timeout` | **180초** | 300초 | 하드닝 - MFA 대기 시간 단축 |

### 3.4 세션 하드닝

```yaml
authfusion:
  sso:
    session:
      timeout: 1800                    # 30분 (기본 1시간에서 단축)
      max-sessions-per-user: 3         # 3개 (기본 5개에서 축소)
```

| 설정 키 | CC 모드 값 | 기본값 | 변경 사유 |
|---------|-----------|--------|----------|
| `session.timeout` | **1,800초** | 3,600초 | 하드닝 - FTA_SSL.3 요구사항 강화 |
| `session.max-sessions-per-user` | **3** | 5 | 하드닝 - 동시 세션 축소 |

### 3.5 보안 정책 하드닝

```yaml
authfusion:
  sso:
    security:
      max-login-attempts: 3            # 3회 (기본 5회에서 축소)
      lockout-duration: 3600           # 1시간 (기본 30분에서 연장)
      password-min-length: 12          # 12자 (기본 8자에서 증가)
      password-max-length: 128         # 128자 (변경 없음)
      password-history-count: 10       # 10개 (기본 5개에서 증가)
      rate-limit:
        requests-per-second: 5         # 5/초 (기본 10에서 축소)
        burst-size: 10                 # 10 (기본 20에서 축소)
```

| 설정 키 | CC 모드 값 | 기본값 | 변경 사유 |
|---------|-----------|--------|----------|
| `security.max-login-attempts` | **3** | 5 | FIA_AFL.1 - 인증 실패 임계값 강화 |
| `security.lockout-duration` | **3,600초** | 1,800초 | FIA_AFL.1 - 잠금 시간 연장 |
| `security.password-min-length` | **12** | 8 | FIA_SOS.1 - 비밀번호 강도 강화 |
| `security.password-history-count` | **10** | 5 | FIA_SOS.1 - 이전 비밀번호 재사용 방지 강화 |
| `security.rate-limit.requests-per-second` | **5** | 10 | FIA_AFL.1 - 요청 빈도 제한 강화 |
| `security.rate-limit.burst-size` | **10** | 20 | FIA_AFL.1 - 버스트 크기 축소 |

### 3.6 OpenAPI/Swagger 비활성화

```yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

CC 모드에서는 API 문서 엔드포인트를 완전히 비활성화하여 공격 표면을 줄인다.

### 3.7 Actuator 최소 노출

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health               # health만 노출 (info, metrics 제외)
  endpoint:
    health:
      show-details: never             # 상세 정보 비노출
```

### 3.8 로깅 하드닝

```yaml
logging:
  level:
    root: WARN
    com.authfusion.sso: INFO           # 일반 로그 INFO 수준
    com.authfusion.sso.audit: DEBUG    # 감사 로그는 DEBUG 유지
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
```

CC 모드에서는 보안에 민감한 정보가 로그에 노출되지 않도록 로그 수준을 높인다.
단, 감사(audit) 로그는 FAU_GEN.1 요구사항 준수를 위해 상세 기록을 유지한다.

---

## 4. 필수 환경변수

CC 평가 구성에서 반드시 설정해야 하는 환경변수 목록이다.

### 4.1 필수 환경변수

| 환경변수 | 설명 | 예시 값 | 보안 수준 |
|---------|------|---------|----------|
| `SPRING_PROFILES_ACTIVE` | Spring 프로파일 | `cc,docker` | 일반 |
| `AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED` | 확장 기능 비활성화 | `false` | 필수 |
| `AUTHFUSION_KEY_MASTER_SECRET` | RSA 키 암호화 마스터 시크릿 | (32자 이상 랜덤 문자열) | **최고 기밀** |
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://db:5432/authfusion_db?currentSchema=sso` | 기밀 |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자명 | `authfusion` | 기밀 |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | (강력한 비밀번호) | **기밀** |
| `AUTHFUSION_SSO_ISSUER` | OIDC Issuer URL | `https://sso.aines.kr` | 일반 |

### 4.2 조건부 환경변수

LDAP 연동 시 추가 설정:

| 환경변수 | 설명 | 비고 |
|---------|------|------|
| `AUTHFUSION_LDAP_BIND_PASSWORD` | LDAP 바인드 비밀번호 | LDAP 사용 시 필수 |

### 4.3 마스터 시크릿 요구사항

`AUTHFUSION_KEY_MASTER_SECRET`은 RSA 비밀키를 AES-256-GCM으로 암호화하는 데 사용되는 마스터 키의 원본이다.

- **최소 길이**: 32자 이상
- **생성 방법**: `openssl rand -base64 48` 등 암호학적으로 안전한 랜덤 생성기 사용
- **보관**: HashiCorp Vault 또는 동등한 시크릿 관리 도구에 저장
- **접근 제한**: 시스템 관리자만 접근 가능
- **절대 금지**: 소스코드, 설정 파일, Docker Compose 파일에 평문으로 기재 금지
- **키 파생**: SHA-256(masterSecret) → 256-bit AES 키

---

## 5. 비활성화되는 기능 목록

CC 평가 구성에서 비활성화되는 기능의 전체 목록이다.

### 5.1 관리 API 엔드포인트

| 엔드포인트 | 기능 | 비활성화 방식 |
|-----------|------|-------------|
| `POST/GET/PUT/DELETE /api/v1/clients/**` | OAuth2 클라이언트 관리 | denyAll() + Bean 미등록 |
| `POST/GET/PUT/DELETE /api/v1/users/**` | 사용자 관리 (CRUD) | denyAll() + Bean 미등록 |
| `POST/GET/PUT/DELETE /api/v1/roles/**` | 역할(RBAC) 관리 | denyAll() + Bean 미등록 |
| `GET /api/v1/sessions/**` | 세션 조회/관리 | denyAll() + Bean 미등록 |
| `GET /api/v1/audit/statistics` | 감사 통계 대시보드 | denyAll() + Bean 미등록 |

### 5.2 UI 관련 기능

| 기능 | 비활성화 방식 |
|------|-------------|
| Swagger UI (`/swagger-ui/**`) | springdoc 비활성화 + denyAll() |
| OpenAPI Docs (`/api-docs/**`) | springdoc 비활성화 + denyAll() |
| 동의 페이지 (`/consent`) | denyAll() + Bean 미등록 |
| Admin Console (Next.js) | Docker replicas: 0 |

### 5.3 그랜트 타입

| 그랜트 | 상태 | 비고 |
|--------|------|------|
| Authorization Code + PKCE | **활성** (TOE) | 핵심 인증 플로우 |
| Refresh Token | **활성** (TOE) | 토큰 갱신 |
| Client Credentials | **비활성** | @ExtendedFeature, M2M 전용 |

### 5.4 Actuator 엔드포인트

| 엔드포인트 | 기본 모드 | CC 모드 |
|-----------|----------|---------|
| `/actuator/health` | 활성 (상세) | 활성 (최소) |
| `/actuator/info` | 활성 | **비활성** |
| `/actuator/metrics` | 활성 | **비활성** |

---

## 6. 유지되는 기능 목록 (TOE 기능)

CC 평가 구성에서 활성 상태로 유지되는 TOE 보안 기능이다.

### 6.1 OIDC 엔드포인트

| 엔드포인트 | 기능 |
|-----------|------|
| `GET /.well-known/openid-configuration` | OIDC Discovery |
| `GET /.well-known/jwks.json` | JWKS 공개키 세트 |
| `GET/POST /oauth2/authorize` | Authorization Code + PKCE 인가 |
| `POST /oauth2/token` | 토큰 발급/갱신 |
| `GET /oauth2/userinfo` | 사용자 정보 조회 |
| `POST /oauth2/revoke` | 토큰 폐기 |

### 6.2 인증 API

| 엔드포인트 | 기능 |
|-----------|------|
| `POST /api/v1/auth/login` | 사용자 로그인 (비밀번호) |
| `POST /api/v1/auth/mfa/verify` | MFA TOTP 검증 |
| `POST /api/v1/auth/logout` | 로그아웃 |

### 6.3 MFA API

| 엔드포인트 | 기능 |
|-----------|------|
| `POST /api/v1/mfa/setup` | TOTP 설정 |
| `POST /api/v1/mfa/verify-setup` | TOTP 설정 검증 |
| `POST /api/v1/mfa/verify` | TOTP 코드 검증 |
| `GET /api/v1/mfa/status` | MFA 상태 조회 |

### 6.4 감사 로그 API

| 엔드포인트 | 기능 |
|-----------|------|
| `GET /api/v1/audit/events` | 감사 이벤트 조회 (TOE) |

---

## 7. 배포 구성

### 7.1 Docker Compose CC 배포

CC 평가 구성의 표준 배포 방법:

```bash
# CC 모드 배포
AUTHFUSION_KEY_MASTER_SECRET=$(openssl rand -base64 48) \
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
```

이 명령은 다음을 수행한다:

1. PostgreSQL 16 컨테이너 기동
2. HashiCorp Vault 1.17 컨테이너 기동
3. SSO Server 컨테이너 기동 (`cc,docker` 프로파일)
4. Admin Console 비배포 (`replicas: 0`)

### 7.2 독립 실행 배포 (JAR)

```bash
export SPRING_PROFILES_ACTIVE=cc
export AUTHFUSION_KEY_MASTER_SECRET=$(cat /run/secrets/master-key)
export SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/authfusion_db?currentSchema=sso
export SPRING_DATASOURCE_USERNAME=authfusion
export SPRING_DATASOURCE_PASSWORD=$(cat /run/secrets/db-password)
export AUTHFUSION_SSO_ISSUER=https://sso.aines.kr

java -jar authfusion-sso-server-1.0.0.jar
```

### 7.3 네트워크 구성 요구사항

| 구분 | 포트 | 프로토콜 | 접근 범위 |
|------|------|---------|----------|
| SSO Server | 8080 | HTTP (내부) | Nginx → SSO Server |
| Nginx (외부) | 443 | HTTPS (TLS 1.2+) | 인터넷/인트라넷 |
| PostgreSQL | 5432 | PostgreSQL | SSO Server → DB |
| Vault | 8200 | HTTP/HTTPS | SSO Server → Vault |
| LDAP (선택) | 636 | LDAPS | SSO Server → LDAP |

### 7.4 TLS 요구사항

- **최소 TLS 버전**: TLS 1.2
- **권장 TLS 버전**: TLS 1.3
- **인증서**: 신뢰할 수 있는 CA에서 발급받은 X.509 인증서
- **키 교환**: ECDHE 또는 DHE (Forward Secrecy 보장)
- **암호 스위트**: AEAD 암호 스위트 (AES-256-GCM, ChaCha20-Poly1305)

---

## 8. 빌드 구성

### 8.1 CC 빌드 명령

```bash
cd products/sso-server
mvn clean package -Pcc
```

`-Pcc` Maven 프로파일이 활성화하는 추가 작업:

1. **SBOM 생성**: CycloneDX 형식의 소프트웨어 자재 명세서
2. **GPG 서명**: 배포 아티팩트에 대한 디지털 서명
3. **재현 가능 빌드**: 동일 소스 → 동일 바이너리 보장

### 8.2 재현 가능 빌드 검증

```bash
# 빌드 1
mvn clean package -Pcc
sha256sum target/authfusion-sso-server-1.0.0.jar > build1.sha256

# 빌드 2 (동일 환경)
mvn clean package -Pcc
sha256sum target/authfusion-sso-server-1.0.0.jar > build2.sha256

# 해시 비교
diff build1.sha256 build2.sha256  # 일치해야 함
```

### 8.3 SBOM 검증

```bash
# SBOM 확인
cat target/bom.json | jq '.components | length'

# SBOM 서명 검증
gpg --verify target/bom.json.asc target/bom.json
```

---

## 9. 운영 검증 체크리스트

CC 평가 구성이 올바르게 적용되었는지 확인하기 위한 체크리스트이다.

### 9.1 설정 검증

| 검증 항목 | 확인 방법 | 기대 결과 |
|-----------|----------|----------|
| CC 프로파일 활성화 | `curl localhost:8081/actuator/health` | 200 OK (상세 없음) |
| 확장 기능 비활성화 | `curl localhost:8081/api/v1/clients` | 403 Forbidden |
| Swagger 비활성화 | `curl localhost:8081/swagger-ui.html` | 403 Forbidden |
| OIDC Discovery | `curl localhost:8081/.well-known/openid-configuration` | 200 OK (JSON) |
| 토큰 엔드포인트 | `curl -X POST localhost:8081/oauth2/token` | 400 Bad Request (정상) |
| 감사 로그 기록 | DB에서 `SELECT count(*) FROM sso_audit_events` | 0 이상 |

### 9.2 보안 설정 검증

| 검증 항목 | 확인 방법 | 기대 결과 |
|-----------|----------|----------|
| 비밀번호 최소 길이 | 11자 비밀번호로 사용자 생성 시도 | 거부 (12자 미만) |
| 로그인 시도 제한 | 3회 연속 실패 후 4회차 시도 | 계정 잠금 |
| 잠금 지속 시간 | 잠금 후 59분 경과 시 로그인 | 잠금 유지 (1시간) |
| 세션 타임아웃 | 30분 유휴 후 API 호출 | 세션 만료 |
| Actuator info | `curl localhost:8081/actuator/info` | 404 Not Found |

---

## 10. 평가 구성 위반 사례

다음은 CC 평가 구성을 위반하는 대표적인 사례이다.

| 위반 유형 | 설명 | 결과 |
|-----------|------|------|
| `extended-features-enabled=true` | 확장 기능 활성화 | CC 인증 무효 |
| 기본 프로파일로 운영 | `cc` 프로파일 미적용 | CC 인증 무효 |
| 기본 마스터 키 사용 | `AUTHFUSION_KEY_MASTER_SECRET` 미설정 | CC 인증 무효 |
| Admin Console 배포 | CC 모드에서 관리 콘솔 활성화 | CC 인증 무효 |
| HTTP 직접 노출 | TLS 종료 없이 8080 포트 외부 노출 | CC 인증 무효 |
| PostgreSQL 비암호화 연결 | DB 연결에 SSL 미사용 | 보안 위험 |

---

## 11. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
