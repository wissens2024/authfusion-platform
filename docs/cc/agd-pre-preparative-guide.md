# 설치/설정 가이드 (AGD_PRE)

## AuthFusion SSO Server 설치 및 초기 설정 지침서

---

## 1. 개요

본 문서는 AuthFusion SSO Server를 CC(Common Criteria) 평가 구성에 부합하도록 설치하고 초기 설정하기 위한 지침을 제공한다.

### 1.1 대상 독자

- 시스템 설치 담당자
- 보안 관리자

### 1.2 문서 범위

- 사전 요구사항 확인
- TOE 설치 절차
- CC 모드 설정
- 마스터 키 설정
- 초기 관리자 설정
- 설치 검증

---

## 2. 사전 요구사항

### 2.1 하드웨어 요구사항

| 항목 | 최소 사양 | 권장 사양 |
|------|----------|----------|
| CPU | 2 코어 | 4 코어 이상 |
| 메모리 | 4 GB | 8 GB 이상 |
| 디스크 | 20 GB | 50 GB 이상 (감사 로그 포함) |
| 네트워크 | 100 Mbps | 1 Gbps |

### 2.2 소프트웨어 요구사항

| 구성 요소 | 버전 | 비고 |
|-----------|------|------|
| 운영 체제 | RHEL 8/9 또는 Ubuntu 22.04 LTS | 보안 패치 최신 적용 |
| Java Runtime | OpenJDK 17 LTS | JRE 또는 JDK |
| PostgreSQL | 16.x | TLS 활성화 필수 |
| Node.js | 18 LTS 이상 | Admin Console 용 |
| Docker | 24.x 이상 | Docker 배포 시 |
| Docker Compose | 2.20 이상 | Docker 배포 시 |

### 2.3 네트워크 요구사항

| 포트 | 프로토콜 | 용도 | 방향 |
|------|---------|------|------|
| 8080 | HTTPS | SSO Server API | 인바운드 |
| 3000 | HTTPS | Admin Console | 인바운드 |
| 5432 | TCP | PostgreSQL | SSO Server -> DB |
| 8200 | HTTPS | HashiCorp Vault | SSO Server -> Vault |
| 123 | UDP | NTP | 아웃바운드 |

### 2.4 인증서 요구사항

CC 모드에서는 TLS 인증서가 필수이다:

- **서버 인증서**: 신뢰할 수 있는 CA에서 발급된 X.509 인증서
- **키 길이**: RSA-2048 이상 또는 ECC P-256 이상
- **프로토콜**: TLS 1.2 이상만 허용

---

## 3. 설치 절차

### 3.1 배포물 검증

TOE 배포물의 무결성을 검증한다:

```bash
# SHA-256 체크섬 검증
sha256sum -c authfusion-sso-server-1.0.0.sha256

# GPG 서명 검증
gpg --verify authfusion-sso-server-1.0.0.jar.sig authfusion-sso-server-1.0.0.jar
```

### 3.2 방법 1: JAR 직접 설치

#### 3.2.1 설치 디렉터리 구성

```bash
# 설치 디렉터리 생성
sudo mkdir -p /opt/authfusion
sudo mkdir -p /opt/authfusion/config
sudo mkdir -p /opt/authfusion/logs
sudo mkdir -p /opt/authfusion/keys

# 전용 사용자 생성
sudo useradd -r -s /bin/false authfusion
sudo chown -R authfusion:authfusion /opt/authfusion
sudo chmod 750 /opt/authfusion
sudo chmod 700 /opt/authfusion/keys
```

#### 3.2.2 JAR 파일 배치

```bash
sudo cp authfusion-sso-server-1.0.0.jar /opt/authfusion/
sudo chown authfusion:authfusion /opt/authfusion/authfusion-sso-server-1.0.0.jar
sudo chmod 550 /opt/authfusion/authfusion-sso-server-1.0.0.jar
```

#### 3.2.3 systemd 서비스 등록

```bash
sudo cat > /etc/systemd/system/authfusion.service << 'EOF'
[Unit]
Description=AuthFusion SSO Server
After=network.target postgresql.service

[Service]
Type=simple
User=authfusion
Group=authfusion
WorkingDirectory=/opt/authfusion
ExecStart=/usr/bin/java -jar authfusion-sso-server-1.0.0.jar --spring.profiles.active=cc
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal

# 보안 강화 옵션
NoNewPrivileges=true
ProtectSystem=strict
ReadWritePaths=/opt/authfusion/logs /opt/authfusion/keys

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable authfusion
```

### 3.3 방법 2: Docker Compose 설치

#### 3.3.1 Docker Compose 설정

```bash
# docker-compose.yml이 위치한 디렉터리에서
docker compose up -d
```

주요 서비스:
- `sso-server`: SSO Server (포트 8080)
- `postgres`: PostgreSQL 16 (포트 5432)
- `vault`: HashiCorp Vault (포트 8200)

### 3.4 데이터베이스 초기화

Flyway를 통해 데이터베이스 스키마가 자동 마이그레이션된다.
최초 실행 시 필요한 테이블이 자동 생성된다.

```bash
# 마이그레이션 상태 확인
curl -H "Authorization: Bearer <admin-token>" \
  "https://sso.example.com/actuator/flyway"
```

---

## 4. CC 모드 설정

### 4.1 CC 모드 설정 파일

`application-cc.yml` 파일을 생성 또는 수정한다:

```yaml
# /opt/authfusion/config/application-cc.yml
authfusion:
  cc-mode:
    enabled: true

  # HTTPS 강제
  server:
    ssl:
      enabled: true
      key-store: /opt/authfusion/config/keystore.p12
      key-store-type: PKCS12
      key-store-password: ${KEYSTORE_PASSWORD}
      protocol: TLS
      enabled-protocols: TLSv1.2,TLSv1.3
      ciphers: >
        TLS_AES_256_GCM_SHA384,
        TLS_AES_128_GCM_SHA256,
        TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
        TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256

  # 비밀번호 정책
  password:
    min-length: 12
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special: true
    history-count: 5
    max-age-days: 90

  # 계정 잠금
  lockout:
    threshold: 5
    duration: 1800
    reset-after: 3600

  # 세션 정책
  session:
    timeout: 1800
    max-concurrent: 3
    absolute-timeout: 28800

  # 토큰 정책
  token:
    access-token-ttl: 900
    refresh-token-ttl: 3600
    id-token-ttl: 900
    authorization-code-ttl: 300

  # 감사 로그
  audit:
    level: FULL
    retention-days: 90
    integrity-check: true

  # 암호 설정
  crypto:
    key-algorithm: RSA
    key-size: 2048
    signing-algorithm: RS256
    bcrypt-strength: 12
```

### 4.2 TLS 인증서 설정

```bash
# PKCS#12 키스토어 생성 (CA 인증서 사용 시)
openssl pkcs12 -export \
  -in server.crt \
  -inkey server.key \
  -certfile ca.crt \
  -out /opt/authfusion/config/keystore.p12 \
  -name authfusion

# 키스토어 파일 권한 설정
sudo chown authfusion:authfusion /opt/authfusion/config/keystore.p12
sudo chmod 400 /opt/authfusion/config/keystore.p12
```

---

## 5. 마스터 키 설정

### 5.1 마스터 키 개요

마스터 키는 JWT 서명키를 암호화(AES-256-GCM)하는 데 사용된다.
CC 모드에서는 마스터 키를 Vault 또는 HSM에서 관리하는 것을 권장한다.

### 5.2 방법 1: HashiCorp Vault 사용 (권장)

```bash
# Vault 시크릿 저장
vault kv put secret/authfusion master-key=$(openssl rand -hex 32)

# 환경 변수 설정
export AUTHFUSION_MASTER_KEY_SOURCE=vault
export VAULT_ADDR=https://vault.example.com:8200
export VAULT_TOKEN=<vault-token>
export VAULT_SECRET_PATH=secret/data/authfusion
```

### 5.3 방법 2: Thales Luna HSM 사용 (최고 보안)

```bash
# PKCS#11 설정
export AUTHFUSION_MASTER_KEY_SOURCE=hsm
export AUTHFUSION_HSM_LIBRARY=/usr/lib/libCryptoki2_64.so
export AUTHFUSION_HSM_SLOT=0
export AUTHFUSION_HSM_PIN=${HSM_PIN}
export AUTHFUSION_HSM_KEY_LABEL=authfusion-master
```

### 5.4 방법 3: 환경 변수 사용 (CC 모드 비권장)

```bash
# 32바이트 랜덤 키 생성
export AUTHFUSION_MASTER_KEY=$(openssl rand -hex 32)
export AUTHFUSION_MASTER_KEY_SOURCE=env
```

> **경고**: 환경 변수 방식은 CC 모드에서 권장되지 않는다. 평가 시 Vault 또는 HSM 사용이 요구될 수 있다.

---

## 6. 초기 관리자 설정

### 6.1 최초 관리자 계정 생성

TOE 최초 실행 시 초기 관리자 계정이 자동 생성된다:

```yaml
# application-cc.yml 초기 관리자 설정
authfusion:
  init:
    admin:
      username: admin
      password: ${INITIAL_ADMIN_PASSWORD}
      email: admin@example.com
      force-password-change: true
```

> **주의**: `force-password-change: true` 설정으로 초기 로그인 시 반드시 비밀번호를 변경해야 한다.

### 6.2 초기 로그인 및 비밀번호 변경

1. Admin Console(`https://sso.example.com:3000`)에 접속
2. 초기 관리자 자격증명으로 로그인
3. 비밀번호 변경 화면에서 CC 모드 비밀번호 정책에 부합하는 새 비밀번호 설정
4. 비밀번호 변경 완료 확인

### 6.3 추가 관리자 계정 생성

```bash
# API를 통한 관리자 계정 생성
curl -X POST -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "secadmin",
    "password": "임시비밀번호!",
    "email": "secadmin@example.com",
    "roles": ["ADMIN"],
    "forcePasswordChange": true
  }' \
  "https://sso.example.com/api/v1/users"
```

---

## 7. 설치 검증

### 7.1 기본 동작 확인

```bash
# 1. 서비스 상태 확인
curl -k https://localhost:8080/actuator/health

# 2. OIDC Discovery 확인
curl -k https://localhost:8080/.well-known/openid-configuration

# 3. JWKS 엔드포인트 확인
curl -k https://localhost:8080/.well-known/jwks.json

# 4. CC 모드 활성화 확인
curl -k https://localhost:8080/actuator/health | jq '.components.ccMode'
```

### 7.2 보안 기능 확인 체크리스트

| 항목 | 확인 방법 | 기대 결과 |
|------|----------|----------|
| HTTPS 강제 | HTTP로 접속 시도 | HTTPS로 리다이렉트 |
| CC 모드 활성화 | health 엔드포인트 확인 | `ccModeEnabled: true` |
| 비밀번호 정책 | 단순 비밀번호로 계정 생성 시도 | 거부됨 |
| 계정 잠금 | 5회 연속 로그인 실패 | 계정 잠금 및 감사 로그 기록 |
| 감사 로그 기록 | 로그인 성공/실패 후 감사 로그 조회 | 이벤트 기록 확인 |
| 세션 타임아웃 | 30분 비활성 후 API 호출 | 401 Unauthorized |
| JWT 서명 검증 | JWKS로 토큰 서명 검증 | 검증 성공 |

### 7.3 설치 완료 기록

설치 완료 후 다음 정보를 기록한다:

- 설치 일시
- 설치 담당자
- TOE 버전
- CC 모드 활성화 여부
- 마스터 키 소스 (Vault/HSM/ENV)
- TLS 인증서 정보 (발급 CA, 유효 기간)
- 설치 검증 결과

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
