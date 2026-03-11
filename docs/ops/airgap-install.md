# 폐쇄망 설치 가이드 (Air-Gap Installation Guide)

## AuthFusion SSO Server v1.0

---

## 1. 개요

본 문서는 인터넷 접근이 불가능한 폐쇄망(Air-Gapped) 환경에서 AuthFusion SSO Server를 설치하고
운영하기 위한 가이드이다. 보안 등급이 높은 기관, 금융기관, 군사 네트워크 등에서 CC 인증 제품을
배포할 때 참고한다.

### 1.1 대상 환경

- 인터넷 접근 완전 차단 환경
- 내부 네트워크만 사용 가능
- Docker Hub, Maven Central, npm 등 외부 저장소 접근 불가
- 매체 반입(USB/DVD) 또는 내부 파일 전송 수단 사용

### 1.2 전제 조건

- 인터넷 접근 가능한 빌드 스테이징 서버 (에어갭 번들 생성용)
- 폐쇄망 서버: Linux (RHEL 8+ 또는 Ubuntu 22.04+)
- Docker / Docker Compose 설치 완료 (또는 Java 17 직접 설치)
- GPG 공개키 사전 반입 (서명 검증용)

---

## 2. 배포 아키텍처

```
┌──────────────────────────────────────────────────────────────────┐
│               인터넷 연결 환경 (빌드 스테이징 서버)                │
│                                                                  │
│  ┌─────────────────┐    ┌─────────────────────────────────────┐ │
│  │ 소스코드 빌드    │───>│ 에어갭 번들 생성                    │ │
│  │ mvn package -Pcc │    │ - SSO Server JAR                   │ │
│  └─────────────────┘    │ - Docker 이미지 (tar)               │ │
│                          │ - SBOM + GPG 서명                   │ │
│                          │ - 체크섬 (SHA-256)                  │ │
│                          │ - 설정 템플릿                       │ │
│                          └─────────────┬───────────────────────┘ │
└────────────────────────────────────────┼─────────────────────────┘
                                         │
                                  USB / DVD / 보안 매체
                                         │
┌────────────────────────────────────────┼─────────────────────────┐
│               폐쇄망 환경              │                          │
│                                        ▼                         │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ 1. 서명/체크섬 검증                                      │   │
│  │ 2. Docker 이미지 로드 (또는 JAR 배치)                    │   │
│  │ 3. 설정 파일 구성                                        │   │
│  │ 4. 서비스 기동                                           │   │
│  │ 5. 기동 후 검증                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │SSO Server│  │PostgreSQL│  │ Vault    │  │ 내부 NTP     │    │
│  │ :8080    │  │ :5432    │  │ :8200    │  │              │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────┘    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. 에어갭 번들 생성 (인터넷 환경)

### 3.1 빌드 환경 준비

인터넷 접근 가능한 빌드 서버에서 수행한다.

```bash
# 필수 도구: Java 17, Maven 3.9+, Docker, GPG
# 소스코드 클론
git clone https://github.com/authfusion/authfusion-platform.git
cd authfusion-platform
```

### 3.2 SSO Server CC 빌드

```bash
cd products/sso-server
mvn clean package -Pcc -DskipTests

# 빌드 결과물 확인
ls -la target/authfusion-sso-server-1.0.0.jar  # 실행 가능 JAR
ls -la target/bom.json                           # CycloneDX SBOM
```

### 3.3 Docker 이미지 빌드 및 내보내기

```bash
# SSO Server Docker 이미지 빌드
docker build -t authfusion-sso-server:1.0.0 ./products/sso-server/

# 필요한 모든 Docker 이미지를 tar로 내보내기
mkdir -p airgap-bundle/images
docker save authfusion-sso-server:1.0.0 -o airgap-bundle/images/authfusion-sso-server-1.0.0.tar
docker save postgres:16 -o airgap-bundle/images/postgres-16.tar
docker save hashicorp/vault:1.17 -o airgap-bundle/images/vault-1.17.tar
```

### 3.4 SSO Agent 빌드

```bash
cd products/sso-agent
mvn clean package -DskipTests

cp target/authfusion-sso-agent-1.0.0.jar ../airgap-bundle/artifacts/
```

### 3.5 번들 디렉터리 구성

```bash
mkdir -p airgap-bundle/{images,artifacts,config,certs,tools,docs}

# 애플리케이션 아티팩트
cp products/sso-server/target/authfusion-sso-server-1.0.0.jar airgap-bundle/artifacts/
cp products/sso-server/target/bom.json airgap-bundle/artifacts/
cp products/sso-agent/target/authfusion-sso-agent-1.0.0.jar airgap-bundle/artifacts/

# 설정 파일 템플릿
cp docker-compose.yml airgap-bundle/config/
cp docker-compose.cc.yml airgap-bundle/config/

# 환경변수 템플릿 생성
cat > airgap-bundle/config/env.template << 'ENVEOF'
# === AuthFusion SSO Server - CC Mode Environment Variables ===
# 이 파일을 .env로 복사하고 값을 설정하세요.

SPRING_PROFILES_ACTIVE=cc,docker
AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED=false

# [필수] 마스터 시크릿 (32자 이상, openssl rand -base64 48 로 생성)
AUTHFUSION_KEY_MASTER_SECRET=CHANGE_ME

# [필수] 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/authfusion_sso
SPRING_DATASOURCE_USERNAME=authfusion
SPRING_DATASOURCE_PASSWORD=CHANGE_ME
POSTGRES_USER=authfusion
POSTGRES_PASSWORD=CHANGE_ME
POSTGRES_DB=authfusion_sso

# [필수] OIDC Issuer (실제 도메인으로 변경)
AUTHFUSION_SSO_ISSUER=https://sso.internal.example.com
ENVEOF

# 문서
cp docs/ops/hardening-guide.md airgap-bundle/docs/
cp docs/cc/evaluated-configuration.md airgap-bundle/docs/
```

### 3.6 체크섬 및 GPG 서명

```bash
cd airgap-bundle

# SHA-256 체크섬 생성
find . -type f \
    -not -name "*.sha256" \
    -not -name "*.asc" \
    -exec sha256sum {} + > checksums.sha256

# GPG 서명 (릴리스 키로 서명)
gpg --armor --detach-sign checksums.sha256
gpg --armor --detach-sign artifacts/authfusion-sso-server-1.0.0.jar
gpg --armor --detach-sign artifacts/bom.json

# 번들 검증 스크립트 생성
cat > tools/verify-bundle.sh << 'SCRIPT'
#!/bin/bash
echo "=== AuthFusion Air-Gap Bundle Verification ==="
echo ""
echo "[1] Verifying GPG signature..."
gpg --verify checksums.sha256.asc checksums.sha256
if [ $? -ne 0 ]; then echo "FAILED: GPG signature invalid"; exit 1; fi
echo ""
echo "[2] Verifying checksums..."
sha256sum -c checksums.sha256
if [ $? -ne 0 ]; then echo "FAILED: Checksum mismatch"; exit 1; fi
echo ""
echo "=== All verifications PASSED ==="
SCRIPT
chmod +x tools/verify-bundle.sh

# 최종 번들 압축
cd ..
tar czf authfusion-airgap-1.0.0.tar.gz airgap-bundle/
sha256sum authfusion-airgap-1.0.0.tar.gz > authfusion-airgap-1.0.0.tar.gz.sha256
```

### 3.7 번들 내용물 목록

```
authfusion-airgap-1.0.0.tar.gz
└── airgap-bundle/
    ├── images/
    │   ├── authfusion-sso-server-1.0.0.tar   # SSO Server Docker 이미지
    │   ├── postgres-16.tar                     # PostgreSQL 16 이미지
    │   └── vault-1.17.tar                      # Vault 1.17 이미지 (선택)
    ├── artifacts/
    │   ├── authfusion-sso-server-1.0.0.jar     # SSO Server JAR
    │   ├── authfusion-sso-server-1.0.0.jar.asc # JAR GPG 서명
    │   ├── authfusion-sso-agent-1.0.0.jar      # SSO Agent JAR
    │   ├── bom.json                            # CycloneDX SBOM
    │   └── bom.json.asc                        # SBOM GPG 서명
    ├── config/
    │   ├── docker-compose.yml                  # Docker Compose 기본
    │   ├── docker-compose.cc.yml               # CC 모드 오버라이드
    │   └── env.template                        # 환경변수 템플릿
    ├── certs/                                  # TLS 인증서 배치 위치
    ├── tools/
    │   └── verify-bundle.sh                    # 번들 검증 스크립트
    ├── docs/
    │   ├── hardening-guide.md
    │   └── evaluated-configuration.md
    ├── checksums.sha256                        # 전체 체크섬
    └── checksums.sha256.asc                    # GPG 서명
```

---

## 4. 폐쇄망 설치 (Docker 방식)

### 4.1 단계 1: 매체 반입 및 검증

```bash
# USB/DVD에서 번들 복사
cp /media/usb/authfusion-airgap-1.0.0.tar.gz /opt/install/
cd /opt/install

# 번들 체크섬 검증
sha256sum -c authfusion-airgap-1.0.0.tar.gz.sha256
# 기대: authfusion-airgap-1.0.0.tar.gz: OK

# 압축 해제
tar xzf authfusion-airgap-1.0.0.tar.gz
cd airgap-bundle

# GPG 공개키 임포트 (최초 1회)
gpg --import /media/usb/authfusion-release-key.pub

# 번들 전체 검증
bash tools/verify-bundle.sh
```

### 4.2 단계 2: Docker 이미지 로드

```bash
# Docker 이미지 로드 (인터넷 불필요)
docker load -i images/authfusion-sso-server-1.0.0.tar
docker load -i images/postgres-16.tar
docker load -i images/vault-1.17.tar  # 선택

# 로드 확인
docker images | grep -E "authfusion|postgres|vault"
```

### 4.3 단계 3: 환경 설정

```bash
# 운영 디렉터리 구성
mkdir -p /opt/authfusion/{config,data,logs,certs}

# 환경변수 파일 생성
cp config/env.template /opt/authfusion/config/.env

# .env 파일 편집 - 반드시 모든 CHANGE_ME 값을 변경
vi /opt/authfusion/config/.env
# - AUTHFUSION_KEY_MASTER_SECRET: openssl rand -base64 48 결과값
# - SPRING_DATASOURCE_PASSWORD / POSTGRES_PASSWORD: 동일한 강력한 비밀번호
# - AUTHFUSION_SSO_ISSUER: 실제 서비스 도메인

# 환경변수 파일 보호
chmod 600 /opt/authfusion/config/.env
```

### 4.4 단계 4: Docker Compose 구성

```bash
# Docker Compose 파일 복사
cp config/docker-compose.yml /opt/authfusion/
cp config/docker-compose.cc.yml /opt/authfusion/
```

**폐쇄망용 docker-compose.yml 수정 사항:**

`build:` 지시어를 `image:`로 교체한다 (이미 로드된 이미지 사용).

```yaml
services:
  postgres:
    image: postgres:16                    # build 대신 image
    container_name: authfusion-postgres
    restart: unless-stopped
    ports:
      - "127.0.0.1:5432:5432"            # localhost 바인딩
    env_file:
      - ./config/.env
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U authfusion -d authfusion_sso"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  sso-server:
    image: authfusion-sso-server:1.0.0    # build 대신 image
    container_name: authfusion-sso-server
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:8080"            # localhost 바인딩
    env_file:
      - ./config/.env
    depends_on:
      postgres:
        condition: service_healthy
    volumes:
      - sso-logs:/app/logs
    healthcheck:
      test: ["CMD-SHELL", "wget -q --spider http://localhost:8080/actuator/health || exit 1"]
      interval: 15s
      timeout: 10s
      retries: 5
      start_period: 45s

volumes:
  postgres-data:
  sso-logs:
```

### 4.5 단계 5: 서비스 기동

```bash
cd /opt/authfusion

# CC 모드로 기동
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d

# 기동 상태 확인
docker compose ps

# 로그 실시간 확인
docker compose logs -f sso-server
```

### 4.6 단계 6: 기동 후 검증

```bash
# Health Check
curl -s http://localhost:8080/actuator/health
# 기대: {"status":"UP"}

# OIDC Discovery
curl -s http://localhost:8080/.well-known/openid-configuration | python3 -m json.tool

# 확장 기능 비활성화 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/clients
# 기대: 403

# Swagger 비활성화 확인
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
# 기대: 403

# JWKS 엔드포인트 확인
curl -s http://localhost:8080/.well-known/jwks.json | python3 -m json.tool
```

---

## 5. 폐쇄망 설치 (JAR 직접 설치)

Docker를 사용할 수 없는 환경에서의 직접 설치 방법이다.

### 5.1 필수 패키지 설치

번들에 Java 17, PostgreSQL 16 RPM/DEB 패키지를 포함하여 설치한다.

```bash
# RHEL 8+
rpm -ivh /opt/install/packages/java-17-openjdk-*.rpm
rpm -ivh /opt/install/packages/postgresql16-server-*.rpm
rpm -ivh /opt/install/packages/postgresql16-*.rpm

# PostgreSQL 초기화
/usr/pgsql-16/bin/postgresql-16-setup initdb
systemctl enable postgresql-16
systemctl start postgresql-16
```

### 5.2 데이터베이스 설정

```bash
sudo -u postgres psql << 'EOF'
CREATE DATABASE authfusion_sso;
CREATE USER authfusion WITH PASSWORD 'strong-password-here';
GRANT ALL PRIVILEGES ON DATABASE authfusion_sso TO authfusion;
\c authfusion_sso
GRANT ALL ON SCHEMA public TO authfusion;
EOF
```

### 5.3 SSO Server 배치 및 기동

```bash
# 전용 사용자 생성
useradd -r -s /usr/sbin/nologin -d /opt/authfusion authfusion

# 디렉터리 구성
mkdir -p /opt/authfusion/{bin,logs}
cp artifacts/authfusion-sso-server-1.0.0.jar /opt/authfusion/bin/
chown -R authfusion:authfusion /opt/authfusion

# 환경변수 파일
cat > /etc/authfusion/env << 'EOF'
SPRING_PROFILES_ACTIVE=cc
AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED=false
AUTHFUSION_KEY_MASTER_SECRET=<마스터-시크릿-여기에-입력>
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/authfusion_sso
SPRING_DATASOURCE_USERNAME=authfusion
SPRING_DATASOURCE_PASSWORD=<DB-비밀번호>
AUTHFUSION_SSO_ISSUER=https://sso.internal.example.com
EOF
chmod 600 /etc/authfusion/env

# systemd 서비스 등록 (하드닝 가이드 참조)
# 서비스 기동
systemctl daemon-reload
systemctl enable authfusion-sso
systemctl start authfusion-sso
journalctl -u authfusion-sso -f
```

---

## 6. 내부 NTP 구성

폐쇄망에서는 외부 NTP 서버에 접근할 수 없으므로 내부 NTP를 구성한다.
JWT 토큰 유효성 검증과 감사 로그 타임스탬프에 정확한 시간이 필수이다.

```bash
# 내부 NTP 서버 (지정된 호스트)
# /etc/chrony.conf
local stratum 10
allow 10.0.0.0/8

# NTP 클라이언트 (SSO Server 호스트)
# /etc/chrony.conf
server ntp.internal.example.com iburst
makestep 1.0 3
rtcsync

systemctl enable chronyd && systemctl start chronyd
chronyc tracking
```

---

## 7. 폐쇄망 업데이트 절차

### 7.1 패치/업그레이드

```
1. [인터넷 환경] 새 버전 빌드: mvn clean package -Pcc
2. [인터넷 환경] 에어갭 번들 생성 (3장 절차)
3. [매체 반입] USB/DVD로 폐쇄망에 이동
4. [폐쇄망] 서명 및 체크섬 검증
5. [폐쇄망] DB 백업:
   docker exec authfusion-postgres pg_dump -U authfusion authfusion_sso > backup.sql
6. [폐쇄망] 서비스 중지:
   docker compose down
7. [폐쇄망] Docker 이미지 교체:
   docker load -i images/authfusion-sso-server-1.1.0.tar
8. [폐쇄망] docker-compose.yml 이미지 태그 업데이트
9. [폐쇄망] 서비스 재기동 (Flyway 자동 마이그레이션):
   docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
10. [폐쇄망] 검증 테스트 수행
11. [보관] 이전 버전 아티팩트 보관 (롤백 대비)
```

### 7.2 롤백

```bash
# 이전 Docker 이미지 로드
docker load -i images/authfusion-sso-server-1.0.0.tar

# DB 복원 (Flyway 마이그레이션 되돌리기)
docker exec -i authfusion-postgres psql -U authfusion authfusion_sso < backup.sql

# 이전 버전으로 기동
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
```

---

## 8. 문제 해결

### 8.1 Docker 이미지 로드 실패

```bash
# 파일 무결성 확인
sha256sum images/authfusion-sso-server-1.0.0.tar
# checksums.sha256의 값과 비교

# Docker 디스크 공간 확인
docker system df
df -h /var/lib/docker
```

### 8.2 DB 연결 실패

```bash
# PostgreSQL 상태 확인
docker compose logs postgres
# 또는
systemctl status postgresql-16

# 연결 테스트
docker exec authfusion-postgres pg_isready -U authfusion
```

### 8.3 Flyway 마이그레이션 오류

```bash
# SSO Server 로그에서 마이그레이션 오류 확인
docker compose logs sso-server | grep -i flyway

# 수동 마이그레이션 상태 확인
docker exec authfusion-postgres psql -U authfusion -d authfusion_sso \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"
```

### 8.4 시간 동기화 문제

```bash
# NTP 동기화 상태 확인
chronyc tracking
chronyc sources

# 시간 수동 설정 (비상시)
date -s "2026-03-04 10:00:00"
hwclock --systohc
```

---

## 9. 보안 매체 관리

### 9.1 반입 절차 체크리스트

| # | 단계 | 담당 | 완료 |
|---|------|------|------|
| 1 | 에어갭 번들 생성 (인터넷 환경) | 개발팀 | [ ] |
| 2 | SHA-256 체크섬 + GPG 서명 첨부 | 개발팀 | [ ] |
| 3 | 보안 매체(USB) 준비 | 보안팀 | [ ] |
| 4 | 매체 바이러스 검사 | 보안팀 | [ ] |
| 5 | 보안 승인 절차 수행 | 보안책임자 | [ ] |
| 6 | 폐쇄망 반입 | 운영팀 | [ ] |
| 7 | 체크섬 + GPG 서명 검증 | 운영팀 | [ ] |
| 8 | 설치 수행 | 운영팀 | [ ] |
| 9 | 기동 후 검증 | 운영팀 | [ ] |
| 10 | 매체 포맷/파기 | 보안팀 | [ ] |
| 11 | 반입 기록 작성 | 보안팀 | [ ] |

### 9.2 반입 기록 양식

| 항목 | 내용 |
|------|------|
| 반입 일자 | YYYY-MM-DD |
| 반입 제품 | AuthFusion SSO Server v1.0.0 |
| 번들 체크섬 | SHA-256: (64자 해시값) |
| GPG 서명 검증 | 통과 / 실패 |
| 승인자 | (성명, 직위) |
| 설치 담당자 | (성명, 직위) |
| 매체 처리 | 포맷 / 물리적 파기 |

---

## 10. 변경 이력

| 버전 | 일자 | 변경 내용 | 작성자 |
|------|------|----------|--------|
| 1.0 | 2026-03-04 | 최초 작성 | AuthFusion Team |
