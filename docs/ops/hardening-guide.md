# 하드닝 가이드 (Hardening Guide)

## AuthFusion SSO Server v1.0 - 프로덕션 배포 보안 강화

---

## 1. 개요

본 문서는 AuthFusion SSO Server를 프로덕션 환경에 안전하게 배포하기 위한 하드닝 가이드이다.
CC(Common Criteria) 평가 구성 준수를 기본으로 하며, 운영환경의 보안 강화 지침을 포함한다.

### 1.1 대상 독자

- SSO Server 운영 담당자
- 보안 관리자
- 인프라/DevOps 엔지니어

### 1.2 전제 조건

- Linux 서버 (RHEL 8+ 또는 Ubuntu 22.04+)
- Java 17+ (OpenJDK LTS)
- PostgreSQL 16+
- Docker / Docker Compose (컨테이너 배포 시)
- Nginx 또는 동등한 리버스 프록시
- NTP 서버 접근 가능

---

## 2. CC 모드 활성화

### 2.1 Spring 프로파일 설정

CC 평가 구성을 적용하려면 `cc` 프로파일을 반드시 활성화한다.

```bash
# 환경변수 방식 (권장)
export SPRING_PROFILES_ACTIVE=cc

# JVM 인수 방식
java -Dspring.profiles.active=cc -jar authfusion-sso-server-1.0.0.jar

# Docker Compose CC 오버라이드 방식
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
# → SPRING_PROFILES_ACTIVE: cc,docker 자동 설정
```

### 2.2 확장 기능 비활성화

```bash
export AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED=false
```

이 설정은 다음을 비활성화한다:
- 관리 API (`/api/v1/clients`, `/api/v1/users`, `/api/v1/roles`, `/api/v1/sessions`)
- 감사 통계 API (`/api/v1/audit/statistics`)
- Swagger UI / OpenAPI 문서
- Client Credentials 그랜트
- 동의 페이지 (`/consent`)

### 2.3 CC 모드 빌드

```bash
cd products/sso-server
mvn clean package -Pcc

# -Pcc Maven 프로파일이 수행하는 추가 작업:
# 1. CycloneDX SBOM 생성
# 2. GPG 서명
# 3. 재현 가능 빌드 설정
```

### 2.4 CC 모드 검증

```bash
# 관리 API 차단 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/clients
# 기대 결과: 403

# OIDC Discovery 정상 확인
curl -s http://localhost:8080/.well-known/openid-configuration | python3 -m json.tool

# Swagger UI 차단 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
# 기대 결과: 403

# Actuator info 차단 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/info
# 기대 결과: 404

# Actuator health 최소 정보 확인
curl -s http://localhost:8080/actuator/health
# 기대 결과: {"status":"UP"} (상세 없음)
```

---

## 3. 마스터 키 관리

### 3.1 마스터 시크릿 생성

RSA 비밀키를 AES-256-GCM으로 암호화하는 데 사용되는 마스터 시크릿이다.
SHA-256(masterSecret) → 256-bit AES 키로 파생된다.

```bash
# 암호학적으로 안전한 48바이트(64자 Base64) 랜덤 문자열 생성
openssl rand -base64 48
```

**요구사항:**
- 최소 32자 이상 (SHA-256 입력으로 충분한 엔트로피)
- 암호학적 랜덤 생성기 사용 (`openssl rand`, `/dev/urandom`)
- 소스코드, 설정 파일, Docker Compose에 평문 포함 **절대 금지**
- 기본값(`authfusion-default-master-key-change-me-in-production`) 사용 시 경고 로그 출력

### 3.2 시크릿 주입 방법

**방법 1: HashiCorp Vault (최고 권장)**

```bash
# Vault에 시크릿 저장
vault kv put secret/authfusion master-key=$(openssl rand -base64 48)

# 환경변수로 주입
export AUTHFUSION_KEY_MASTER_SECRET=$(vault kv get -field=master-key secret/authfusion)
```

**방법 2: Docker Secrets**

```bash
# 시크릿 생성
echo -n "$(openssl rand -base64 48)" | docker secret create authfusion_master_key -

# docker-compose.yml에서 시크릿 마운트
services:
  sso-server:
    secrets:
      - authfusion_master_key
```

**방법 3: 파일 기반 (최소 요구)**

```bash
# 시크릿 파일 생성 (root 소유, 0400 권한)
openssl rand -base64 48 > /etc/authfusion/master-key
chmod 0400 /etc/authfusion/master-key
chown root:root /etc/authfusion/master-key

# 환경변수 설정 파일에서 참조
AUTHFUSION_KEY_MASTER_SECRET=$(cat /etc/authfusion/master-key)
```

### 3.3 마스터 키 로테이션 주의사항

마스터 키를 변경하면 기존에 암호화된 서명키를 복호화할 수 없게 된다.
마스터 키 변경 절차:

```
1. 기존 마스터 키로 서버가 가동 중인 상태에서 시작
2. DB의 sso_signing_keys에서 모든 키를 기존 마스터 키로 복호화
3. 새 마스터 키로 재암호화하여 DB 업데이트
4. 새 마스터 키로 환경변수 변경 후 서버 재기동
```

---

## 4. 데이터베이스 하드닝

### 4.1 PostgreSQL 접근 제어

```conf
# pg_hba.conf - SSO Server IP만 허용, 나머지 차단
# TYPE  DATABASE        USER        ADDRESS         METHOD
host    authfusion_sso  authfusion  10.0.1.10/32    scram-sha-256
host    authfusion_sso  backup      10.0.1.20/32    scram-sha-256
host    all             all         0.0.0.0/0       reject
```

### 4.2 SSL/TLS 연결

```conf
# postgresql.conf
ssl = on
ssl_cert_file = '/etc/postgresql/server.crt'
ssl_key_file = '/etc/postgresql/server.key'
ssl_ca_file = '/etc/postgresql/ca.crt'
ssl_min_protocol_version = 'TLSv1.2'
```

```yaml
# SSO Server JDBC URL에 SSL 파라미터 추가
spring:
  datasource:
    url: jdbc:postgresql://db:5432/authfusion_sso?ssl=true&sslmode=verify-full&sslrootcert=/app/certs/ca.crt
```

### 4.3 사용자 권한 최소화

```sql
-- SSO Server 전용 사용자 (DML만 허용)
CREATE ROLE authfusion WITH LOGIN PASSWORD 'strong-password';
GRANT CONNECT ON DATABASE authfusion_sso TO authfusion;
GRANT USAGE ON SCHEMA public TO authfusion;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO authfusion;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authfusion;

-- 감사 테이블 DELETE 금지 (무결성 보장)
REVOKE DELETE ON sso_audit_events FROM authfusion;

-- Flyway 마이그레이션 전용 사용자 (DDL 허용)
CREATE ROLE authfusion_admin WITH LOGIN PASSWORD 'admin-password';
GRANT ALL PRIVILEGES ON DATABASE authfusion_sso TO authfusion_admin;
```

### 4.4 PostgreSQL 하드닝 설정

```conf
# postgresql.conf 추가 하드닝
log_connections = on
log_disconnections = on
log_statement = 'ddl'
log_min_duration_statement = 1000  # 1초 이상 쿼리 로깅
password_encryption = scram-sha-256
```

### 4.5 정기 백업

```bash
# 일일 자동 백업 (cron)
0 2 * * * pg_dump -U backup -h localhost authfusion_sso | gzip > /backup/authfusion_$(date +\%Y\%m\%d).sql.gz

# 백업 보존 기간: 최소 90일
find /backup -name "authfusion_*.sql.gz" -mtime +90 -delete

# 백업 복구 테스트 (월 1회 권장)
gunzip -c /backup/authfusion_20260301.sql.gz | psql -U authfusion_admin -d authfusion_sso_test
```

---

## 5. 네트워크 하드닝

### 5.1 네트워크 토폴로지

```
┌──────────────────────────────────────────────┐
│              DMZ (외부 접근 가능)              │
│  ┌──────────┐                                │
│  │ Nginx    │ :443 (HTTPS, TLS 1.2+)        │
│  └────┬─────┘                                │
└───────┼──────────────────────────────────────┘
        │ :8080 (HTTP, 내부 전용)
┌───────┼──────────────────────────────────────┐
│       │  내부 네트워크 (외부 접근 차단)        │
│  ┌────▼─────┐  ┌──────────┐  ┌──────────┐   │
│  │SSO Server│  │PostgreSQL│  │ Vault    │   │
│  │ :8080    │  │ :5432    │  │ :8200    │   │
│  └──────────┘  └──────────┘  └──────────┘   │
└──────────────────────────────────────────────┘
```

### 5.2 방화벽 규칙

| 출발지 | 목적지 | 포트 | 허용 여부 |
|--------|--------|------|----------|
| 인터넷 | Nginx | 443/tcp | **허용** |
| 인터넷 | SSO Server | 8080/tcp | **차단** |
| 인터넷 | PostgreSQL | 5432/tcp | **차단** |
| 인터넷 | Vault | 8200/tcp | **차단** |
| Nginx | SSO Server | 8080/tcp | 허용 |
| SSO Server | PostgreSQL | 5432/tcp | 허용 |
| SSO Server | Vault | 8200/tcp | 허용 |
| SSO Server | LDAP | 636/tcp | 허용 (선택) |
| SSO Server | NTP | 123/udp | 허용 |

### 5.3 Nginx TLS 구성

```nginx
server {
    listen 443 ssl http2;
    server_name sso.example.com;

    # TLS 설정
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305';
    ssl_prefer_server_ciphers on;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:50m;
    ssl_session_tickets off;

    # 인증서
    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_trusted_certificate /etc/nginx/ssl/chain.pem;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;

    # 보안 헤더
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;" always;

    # 프록시 설정
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 1m;
        proxy_connect_timeout 5s;
        proxy_read_timeout 30s;
    }

    # Actuator 외부 차단
    location /actuator {
        deny all;
        return 403;
    }
}

# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name sso.example.com;
    return 301 https://$host$request_uri;
}
```

---

## 6. 인증 보안 하드닝

### 6.1 CC 모드 보안 매개변수

`application-cc.yml`에서 자동 적용되는 하드닝 설정:

| 매개변수 | 기본값 | CC 모드 값 | 관련 SFR |
|---------|--------|-----------|---------|
| `security.max-login-attempts` | 5 | **3** | FIA_AFL.1 |
| `security.lockout-duration` | 1,800초 | **3,600초** | FIA_AFL.1 |
| `security.password-min-length` | 8 | **12** | FIA_SOS.1 |
| `security.password-history-count` | 5 | **10** | FIA_SOS.1 |
| `security.rate-limit.requests-per-second` | 10 | **5** | FIA_AFL.1 |
| `security.rate-limit.burst-size` | 20 | **10** | FIA_AFL.1 |
| `session.timeout` | 3,600초 | **1,800초** | FTA_SSL.3 |
| `session.max-sessions-per-user` | 5 | **3** | FTA_SSL.3 |
| `jwt.refresh-token-validity` | 86,400초 | **43,200초** | FCS_COP.1 |
| `mfa.pending-session.timeout` | 300초 | **180초** | FIA_UAU.1 |

### 6.2 관리자 계정 MFA 필수화

CC 평가 구성에서는 관리자 계정에 TOTP MFA를 필수로 활성화할 것을 강력히 권장한다.

```bash
# 관리자 계정 MFA 설정 (API 호출)
curl -X POST http://localhost:8080/api/v1/mfa/setup \
  -H "Authorization: Bearer <admin-jwt>" \
  -H "Content-Type: application/json"
# → QR 코드 + 비밀키 + 복구 코드 수신
# → 인증 앱에 등록 후 검증 호출
```

### 6.3 HSM 연동 (선택)

Thales Luna HSM(PKCS#11)을 사용하면 RSA 비밀키를 하드웨어에 저장할 수 있다.
`KeyEncryptionService`를 HSM 인터페이스로 교체하여 적용한다.

---

## 7. JVM 하드닝

### 7.1 JVM 보안 옵션

```bash
java \
  -server \
  -Xms512m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -Djava.security.egd=file:/dev/urandom \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC \
  -Djava.net.preferIPv4Stack=true \
  -Dcom.sun.jndi.ldap.object.trustURLCodebase=false \
  -Dlog4j2.formatMsgNoLookups=true \
  -jar authfusion-sso-server-1.0.0.jar
```

### 7.2 비루트 실행

```bash
# 전용 서비스 사용자 생성
useradd -r -s /usr/sbin/nologin -d /opt/authfusion authfusion
chown -R authfusion:authfusion /opt/authfusion
chmod -R 750 /opt/authfusion
```

---

## 8. 운영체제 하드닝

### 8.1 systemd 서비스 등록

```ini
# /etc/systemd/system/authfusion-sso.service
[Unit]
Description=AuthFusion SSO Server
After=network.target postgresql.service

[Service]
Type=simple
User=authfusion
Group=authfusion
EnvironmentFile=/etc/authfusion/env
ExecStart=/usr/bin/java -server -Xms512m -Xmx1024m -jar /opt/authfusion/bin/authfusion-sso-server-1.0.0.jar
Restart=always
RestartSec=10

# systemd 보안 강화
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/opt/authfusion/logs
PrivateTmp=true
ProtectKernelTunables=true
ProtectControlGroups=true
MemoryDenyWriteExecute=true

[Install]
WantedBy=multi-user.target
```

### 8.2 시간 동기화 (NTP)

감사 로그 타임스탬프와 JWT 토큰 유효성 검증에 정확한 시간이 필수적이다.

```bash
# chrony 설치 및 설정
dnf install chrony  # RHEL 8+
# 또는
apt install chrony  # Ubuntu 22.04+

# /etc/chrony.conf
server ntp1.example.com iburst
server ntp2.example.com iburst
makestep 1.0 3
rtcsync

systemctl enable chronyd && systemctl start chronyd
chronyc tracking  # 동기화 상태 확인
```

---

## 9. 로그 하드닝

### 9.1 로그 파일 보호

```bash
# 로그 디렉터리 권한
mkdir -p /opt/authfusion/logs
chown authfusion:authfusion /opt/authfusion/logs
chmod 750 /opt/authfusion/logs
```

### 9.2 logrotate 설정

```
# /etc/logrotate.d/authfusion
/opt/authfusion/logs/*.log {
    daily
    rotate 90
    compress
    delaycompress
    missingok
    notifempty
    create 640 authfusion authfusion
}
```

### 9.3 SIEM 전송

```bash
# Filebeat 설정 예시
# /etc/filebeat/conf.d/authfusion.yml
filebeat.inputs:
- type: log
  paths:
    - /opt/authfusion/logs/sso-server.log
  fields:
    service: authfusion-sso
output.elasticsearch:
  hosts: ["siem.example.com:9200"]
```

---

## 10. Docker 하드닝

### 10.1 컨테이너 보안

```yaml
# docker-compose.yml 보안 강화
services:
  sso-server:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1024M
        reservations:
          cpus: '0.5'
          memory: 512M
    security_opt:
      - no-new-privileges:true
    read_only: true
    tmpfs:
      - /tmp
    ports:
      - "127.0.0.1:8080:8080"   # localhost만 바인딩

  postgres:
    ports:
      - "127.0.0.1:5432:5432"   # localhost만 바인딩
```

---

## 11. SBOM 및 공급망 보안

### 11.1 SBOM 검증

```bash
# CC 빌드 후 SBOM 확인
cat target/bom.json | python3 -m json.tool

# SBOM 내 의존성 수 확인
cat target/bom.json | python3 -c "import json,sys; d=json.load(sys.stdin); print(len(d.get('components',[])))"

# GPG 서명 검증
gpg --verify target/bom.json.asc target/bom.json
```

### 11.2 재현 가능 빌드 검증

```bash
# 동일 환경에서 두 번 빌드 후 해시 비교
mvn clean package -Pcc && sha256sum target/*.jar > build1.sha256
mvn clean package -Pcc && sha256sum target/*.jar > build2.sha256
diff build1.sha256 build2.sha256  # 일치해야 함
```

---

## 12. 하드닝 체크리스트

### 12.1 필수 항목

| # | 항목 | 확인 방법 | 완료 |
|---|------|----------|------|
| 1 | CC 프로파일 활성화 | `SPRING_PROFILES_ACTIVE=cc` | [ ] |
| 2 | 확장 기능 비활성화 | 관리 API 403 응답 | [ ] |
| 3 | 마스터 시크릿 설정 | 기본값이 아닌 32자+ 랜덤 값 | [ ] |
| 4 | TLS 1.2+ 적용 | Nginx SSL 설정 | [ ] |
| 5 | DB SSL 연결 | JDBC URL `ssl=true` | [ ] |
| 6 | DB 접근 IP 제한 | pg_hba.conf | [ ] |
| 7 | 감사 테이블 DELETE 금지 | DB 권한 확인 | [ ] |
| 8 | NTP 동기화 | `chronyc tracking` | [ ] |
| 9 | 8080/5432 외부 차단 | 방화벽 규칙 | [ ] |
| 10 | 비루트 사용자 실행 | `ps aux | grep sso` | [ ] |

### 12.2 권장 항목

| # | 항목 | 확인 방법 | 완료 |
|---|------|----------|------|
| 11 | Vault 시크릿 관리 | Vault 연동 확인 | [ ] |
| 12 | HSTS 헤더 | curl 응답 헤더 | [ ] |
| 13 | CSP 헤더 | curl 응답 헤더 | [ ] |
| 14 | SIEM 로그 전송 | SIEM 수신 확인 | [ ] |
| 15 | DB 일일 백업 | cron 설정 확인 | [ ] |
| 16 | Docker 리소스 제한 | docker stats | [ ] |
| 17 | Actuator 외부 차단 | Nginx deny 설정 | [ ] |
| 18 | SBOM 서명 검증 | gpg --verify | [ ] |
| 19 | 관리자 MFA 활성화 | MFA status API | [ ] |
| 20 | logrotate 설정 | /etc/logrotate.d/ 확인 | [ ] |

---

## 13. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
