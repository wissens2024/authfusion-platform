# 배포 가이드

## AuthFusion Platform 배포 지침서

---

## 1. 개요

AuthFusion Platform은 3개의 제품으로 구성됩니다:

| 제품 | 경로 | 기술 | 포트 | 역할 |
|------|------|------|------|------|
| SSO Server | `products/sso-server` | Spring Boot 3.2, Java 17 | 8080 | OIDC Provider (TOE) |
| SSO Agent | `products/sso-agent` | Jakarta Servlet, Java 17 | - | 레거시 앱 연동 JAR (TOE) |
| Admin Console | `products/admin-console` | Next.js 14, TypeScript | 3000 | 웹 관리자 콘솔 (TOE) |

### 1.1 배포 방식

| 방식 | 용도 | 권장 환경 |
|------|------|----------|
| 로컬 직접 실행 | 개발/디버깅 | 개발 |
| Docker Compose | 전체 스택 통합 실행 | 개발, 테스트, 소규모 운영 |
| JAR/Node.js 직접 배포 | 개별 서비스 배포 | 운영 (CC 모드) |
| Docker 운영 | 컨테이너 기반 배포 | 운영 |

---

## 2. 사전 요구사항

### 2.1 로컬 개발 환경

```bash
# Java 17
java -version    # openjdk 17.x.x

# Maven 3.9+
mvn -version     # Apache Maven 3.9.x

# Node.js 20+
node -v          # v20.x.x

# npm 10+
npm -v           # 10.x.x

# Docker 24+ (선택)
docker --version
docker compose version
```

### 2.2 외부 의존성

| 서비스 | 용도 | 필수 |
|--------|------|------|
| PostgreSQL 16 | SSO Server 데이터베이스 | O |
| HashiCorp Vault 1.17 | 시크릿 관리 (운영) | 선택 |
| LDAP/AD | 외부 사용자 저장소 | 선택 |
| NTP | 시간 동기화 (TOTP MFA) | O |

---

## 3. 로컬 개발 환경 실행

### 3.1 방법 A: Docker Compose (권장)

가장 간편하게 전체 스택을 실행하는 방법입니다.

```bash
# 프로젝트 루트에서
cd authfusion-platform

# 전체 스택 시작 (PostgreSQL + Vault + SSO Server + Admin Console)
docker compose up -d

# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f sso-server
docker compose logs -f admin-console
```

**접속 URL:**
- SSO Server: http://localhost:8081
- Admin Console: http://localhost:3001
- Vault: http://localhost:8200
- PostgreSQL: localhost:5432

**서비스 관리:**
```bash
# 특정 서비스 재시작
docker compose restart sso-server

# 서비스 종료
docker compose down

# 종료 + 데이터 삭제 (DB 초기화)
docker compose down -v

# 이미지 재빌드
docker compose build --no-cache sso-server
docker compose up -d
```

### 3.2 방법 B: 개별 실행 (디버깅)

IDE에서 디버깅하거나 개별 서비스만 실행할 때 사용합니다.

#### Step 1: PostgreSQL 실행

```bash
# Docker로 PostgreSQL만 실행
docker compose up -d postgres

# 또는 직접 Docker로 실행
docker run -d \
  --name authfusion-postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=authfusion \
  -e POSTGRES_PASSWORD=authfusion-secret \
  -e POSTGRES_DB=authfusion_sso \
  postgres:16
```

#### Step 2: SSO Server 실행

```bash
cd products/sso-server

# Maven으로 실행
mvn spring-boot:run

# 또는 JAR 빌드 후 실행
mvn clean package -DskipTests
java -jar target/authfusion-sso-server-1.0.0-SNAPSHOT.jar

# IDE에서 실행: SsoServerApplication.main() 실행
```

#### Step 3: Admin Console 실행

```bash
cd products/admin-console

# 환경 변수 설정
cp .env.local.example .env.local
# .env.local 편집 (SSO_SERVER_URL=http://localhost:8081)

# 의존성 설치
npm install

# 개발 서버 (Hot Reload)
npm run dev
```

#### Step 4: 실행 확인

```bash
# SSO Server 헬스 체크
curl http://localhost:8081/actuator/health

# OIDC Discovery
curl http://localhost:8081/.well-known/openid-configuration

# JWKS 공개키
curl http://localhost:8081/.well-known/jwks.json

# Swagger UI
open http://localhost:8081/swagger-ui.html

# Admin Console
open http://localhost:3001
```

### 3.3 SSO Agent 개발

SSO Agent는 독립 실행 서비스가 아닌 JAR 라이브러리입니다.

```bash
cd products/sso-agent

# 빌드
mvn clean package

# 로컬 Maven 저장소에 설치
mvn clean install

# 테스트
mvn test
```

테스트 앱에 적용하려면 `products/sso-agent/README.md`의 통합 가이드를 참조하세요.

---

## 4. CC 모드 배포

### 4.1 Docker Compose CC 모드

```bash
# CC 하드닝 프로파일로 실행
export AUTHFUSION_KEY_MASTER_SECRET=<강력한-마스터키>
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
```

CC 모드 주요 차이점:
- `SPRING_PROFILES_ACTIVE=cc,docker`
- 확장 기능 비활성화 (`AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED=false`)
- Admin Console 미배포 (`replicas: 0`)
- 강화된 보안 설정 (로그인 시도 3회, 비밀번호 12자 이상 등)

### 4.2 CC 모드 체크리스트

| 단계 | 항목 | 상태 |
|------|------|------|
| 1 | 배포물 무결성 검증 (SHA-256 + GPG) | [ ] |
| 2 | TLS 인증서 준비 (신뢰 CA 발급) | [ ] |
| 3 | 마스터 키 설정 (`AUTHFUSION_KEY_MASTER_SECRET`) | [ ] |
| 4 | CC 프로파일 활성화 (`spring.profiles.active=cc`) | [ ] |
| 5 | PostgreSQL TLS 연결 설정 | [ ] |
| 6 | 초기 관리자 비밀번호 변경 | [ ] |
| 7 | 방화벽 규칙 설정 | [ ] |
| 8 | NTP 동기화 확인 | [ ] |
| 9 | SIEM 연동 설정 (선택) | [ ] |
| 10 | 설치 검증 수행 | [ ] |

---

## 5. 운영 환경 (도메인) 배포

### 5.1 운영 아키텍처

```
┌─────────── 외부 네트워크 ──────────┐
│                                    │
│  사용자/관리자 ──→ sso.aines.kr:443│
│                                    │
└──────────────────┬─────────────────┘
                   │
    ┌──────────────▼──────────────┐
    │   Nginx (Reverse Proxy)     │
    │  TLS 종단, 보안 헤더        │
    │  경로 기반 라우팅            │
    └──────┬───────────┬──────────┘
           │           │
           │ /admin/*  │ 그 외
           │           │
┌──────────▼──┐  ┌─────▼──────────────┐
│Admin Console│  │ SSO Server         │
│ :3000       │  │ :8080              │
└─────────────┘  └──────┬─────────────┘
                        │
┌───────────────┐  ┌────▼──────────┐  ┌───────────────┐
│ PostgreSQL    │  │ Vault / HSM   │  │ LDAP (선택)   │
│ :5432         │  │ :8200         │  │ :389          │
└───────────────┘  └───────────────┘  └───────────────┘
```

### 5.2 도메인 계획

| 서비스 | 도메인 | 포트 | 경로 | 설명 |
|--------|--------|------|------|------|
| SSO Server | `sso.aines.kr` | 443 | `/` (기본) | OIDC Provider, 로그인 |
| Admin Console | `sso.aines.kr` | 443 | `/admin/*` | 관리자 콘솔 (경로 기반 라우팅) |
| PostgreSQL | 내부 전용 | 5432 | - | 외부 노출 금지 |
| Vault | 내부 전용 | 8200 | - | 외부 노출 금지 |

### 5.3 SSO Server 운영 배포

#### JAR 직접 배포

```bash
# 1. CC 프로파일로 빌드
cd products/sso-server
mvn clean package -Pcc -DskipTests

# 2. 배포
sudo mkdir -p /opt/authfusion/{config,logs}
sudo cp target/authfusion-sso-server-*.jar /opt/authfusion/sso-server.jar
sudo chown -R authfusion:authfusion /opt/authfusion
sudo chmod 550 /opt/authfusion/sso-server.jar

# 3. 운영 설정 파일
sudo cat > /opt/authfusion/config/application-prod.yml << 'EOF'
server:
  port: 8080
  forward-headers-strategy: framework

spring:
  datasource:
    url: jdbc:postgresql://db.internal:5432/authfusion_db?currentSchema=sso&ssl=true&sslmode=verify-full
    username: authfusion
    password: ${DB_PASSWORD}

authfusion:
  sso:
    issuer: https://sso.aines.kr
    cors:
      allowed-origins: https://sso.aines.kr
    ldap:
      enabled: false
EOF

# 4. systemd 서비스
sudo cat > /etc/systemd/system/authfusion-sso.service << 'EOF'
[Unit]
Description=AuthFusion SSO Server
After=network.target postgresql.service

[Service]
Type=simple
User=authfusion
Group=authfusion
WorkingDirectory=/opt/authfusion
ExecStart=/usr/bin/java \
  -Xms512m -Xmx1024m \
  -Dspring.profiles.active=cc,prod \
  -jar /opt/authfusion/sso-server.jar \
  --spring.config.additional-location=file:/opt/authfusion/config/
Environment=AUTHFUSION_KEY_MASTER_SECRET=<마스터키>
Environment=DB_PASSWORD=<DB비밀번호>
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 5. 시작
sudo systemctl daemon-reload
sudo systemctl enable authfusion-sso
sudo systemctl start authfusion-sso
```

#### Docker 운영 배포

```bash
cd products/sso-server

docker build -t authfusion/sso-server:1.0.0 .

docker run -d \
  --name authfusion-sso-server \
  --restart unless-stopped \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=cc \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db.internal:5432/authfusion_db?currentSchema=sso \
  -e SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -e AUTHFUSION_SSO_ISSUER=https://sso.aines.kr \
  -e AUTHFUSION_KEY_MASTER_SECRET=${MASTER_SECRET} \
  -e AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED=false \
  -e AUTHFUSION_SSO_CORS_ALLOWED_ORIGINS=https://sso.aines.kr \
  authfusion/sso-server:1.0.0
```

### 5.4 Admin Console 운영 배포

#### Docker 배포

```bash
cd products/admin-console

docker build -t authfusion/admin-console:1.0.0 .

docker run -d \
  --name authfusion-admin-console \
  --restart unless-stopped \
  -p 3000:3000 \
  -e NEXT_PUBLIC_SSO_SERVER_URL=https://sso.aines.kr \
  -e SSO_SERVER_URL=http://sso-server:8081 \
  -e SSO_CLIENT_ID=admin-console \
  -e SSO_CLIENT_SECRET=${SSO_CLIENT_SECRET} \
  -e NEXTAUTH_URL=https://sso.aines.kr \
  -e NEXTAUTH_SECRET=${NEXTAUTH_SECRET} \
  -e NODE_ENV=production \
  authfusion/admin-console:1.0.0
```

#### Node.js 직접 배포

```bash
cd products/admin-console
npm ci --production=false
npm run build

# standalone 배포
cp -r .next/standalone /opt/authfusion/admin-console/
cp -r .next/static /opt/authfusion/admin-console/.next/static
cp -r public /opt/authfusion/admin-console/public

# systemd 서비스
sudo cat > /etc/systemd/system/authfusion-admin.service << 'EOF'
[Unit]
Description=AuthFusion Admin Console
After=network.target

[Service]
Type=simple
User=authfusion
WorkingDirectory=/opt/authfusion/admin-console
ExecStart=/usr/bin/node server.js
EnvironmentFile=/opt/authfusion/admin-console/.env.production
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable authfusion-admin
sudo systemctl start authfusion-admin
```

### 5.5 SSO Agent 운영 배포

SSO Agent는 레거시 앱에 포함되는 JAR 라이브러리이므로 별도 서비스로 배포하지 않습니다.

```bash
# CC 프로파일로 빌드
cd products/sso-agent
mvn clean package -Pcc

# 대상 앱의 라이브러리 경로에 복사
cp target/sso-agent-1.0.0.jar /opt/my-legacy-app/lib/

# 또는 Maven 저장소에 배포
mvn deploy -Pcc
```

대상 앱의 설정에서 운영 SSO Server URL을 지정합니다:

```yaml
authfusion:
  sso-agent:
    sso-server-url: https://sso.aines.kr
    require-https: true
    session-timeout: 1800
```

---

## 6. Nginx 리버스 프록시 설정

### 6.1 통합 리버스 프록시 (경로 기반 라우팅)

```nginx
# /etc/nginx/conf.d/authfusion.conf

upstream sso_backend {
    server 127.0.0.1:8080;
}

upstream admin_backend {
    server 127.0.0.1:3000;
}

server {
    listen 80;
    server_name sso.aines.kr;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name sso.aines.kr;

    ssl_certificate     /etc/nginx/ssl/sso.aines.kr.crt;
    ssl_certificate_key /etc/nginx/ssl/sso.aines.kr.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;

    # Admin Console (경로 기반 라우팅)
    location /admin/ {
        proxy_pass http://admin_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # SSO Server (기본 경로)
    location / {
        proxy_pass http://sso_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 7. 데이터베이스 설정

### 7.1 PostgreSQL 초기 설정

```sql
-- 데이터베이스 생성
CREATE DATABASE authfusion_sso
  WITH ENCODING='UTF8'
  LC_COLLATE='ko_KR.UTF-8'
  LC_CTYPE='ko_KR.UTF-8'
  TEMPLATE=template0;

-- 전용 사용자 생성
CREATE USER authfusion WITH PASSWORD '<강력한비밀번호>';

-- 권한 부여
GRANT CONNECT ON DATABASE authfusion_sso TO authfusion;
GRANT USAGE ON SCHEMA public TO authfusion;
GRANT CREATE ON SCHEMA public TO authfusion;
```

### 7.2 PostgreSQL TLS 설정 (CC 모드)

```bash
# postgresql.conf
ssl = on
ssl_cert_file = '/var/lib/postgresql/server.crt'
ssl_key_file = '/var/lib/postgresql/server.key'
ssl_ca_file = '/var/lib/postgresql/ca.crt'
ssl_min_protocol_version = 'TLSv1.2'

# pg_hba.conf (TLS 강제)
hostssl authfusion_sso authfusion 0.0.0.0/0 scram-sha-256
```

### 7.3 Flyway 마이그레이션

SSO Server 시작 시 자동으로 적용됩니다:

| 버전 | 내용 |
|------|------|
| V1-V7 | 기본 스키마 (users, clients, roles, sessions, audit, consent, passwords) |
| V8 | 서명키 (`sso_signing_keys`) - AES-256-GCM 암호화 저장 |
| V9 | MFA (`sso_totp_secrets`, `sso_recovery_codes`, `sso_mfa_pending_sessions`) |
| V10 | LDAP 연동 (`user_source`, `external_id`, `ldap_synced_at` 컬럼) |

```bash
# 마이그레이션 상태 확인
curl http://localhost:8081/actuator/flyway

# 마이그레이션 복구
java -jar sso-server.jar --spring.flyway.repair
```

---

## 8. 환경 변수 목록

### 8.1 SSO Server

| 환경 변수 | 기본값 | 설명 |
|----------|--------|------|
| `SERVER_PORT` | 8080 | 서버 포트 |
| `SPRING_PROFILES_ACTIVE` | (기본) | 프로파일 (cc, docker, prod) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/authfusion_db?currentSchema=sso` | DB URL |
| `SPRING_DATASOURCE_USERNAME` | authfusion | DB 사용자명 |
| `SPRING_DATASOURCE_PASSWORD` | authfusion-secret | DB 비밀번호 |
| `AUTHFUSION_SSO_ISSUER` | `http://localhost:8081` | OIDC Issuer URL |
| `AUTHFUSION_KEY_MASTER_SECRET` | (기본값) | 키 암호화 마스터 시크릿 |
| `AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED` | true | 확장 기능 활성화 |
| `AUTHFUSION_SSO_CORS_ALLOWED_ORIGINS` | `http://localhost:3001` | CORS 허용 도메인 |
| `AUTHFUSION_LDAP_BIND_PASSWORD` | - | LDAP 바인드 비밀번호 |

### 8.2 Admin Console

| 환경 변수 | 기본값 | 설명 |
|----------|--------|------|
| `NEXT_PUBLIC_SSO_SERVER_URL` | `http://localhost:8081` | SSO Server URL (브라우저) |
| `SSO_SERVER_URL` | `http://localhost:8081` | SSO Server URL (서버사이드) |
| `SSO_CLIENT_ID` | admin-console | OIDC 클라이언트 ID |
| `SSO_CLIENT_SECRET` | - | OIDC 클라이언트 시크릿 |
| `NEXTAUTH_URL` | `http://localhost:3001` | NextAuth 콜백 URL |
| `NEXTAUTH_SECRET` | - | NextAuth 암호화 키 |
| `NODE_ENV` | development | 실행 환경 |

### 8.3 SSO Agent

| 설정 키 | 기본값 | 설명 |
|---------|--------|------|
| `authfusion.sso-agent.sso-server-url` | `http://localhost:8081` | SSO Server URL |
| `authfusion.sso-agent.client-id` | - | OAuth2 클라이언트 ID |
| `authfusion.sso-agent.client-secret` | - | OAuth2 클라이언트 시크릿 |
| `authfusion.sso-agent.require-https` | false | HTTPS 강제 |
| `authfusion.sso-agent.session-timeout` | 3600 | 세션 타임아웃 (초) |

---

## 9. 모니터링

### 9.1 헬스 체크

```bash
# SSO Server
curl http://localhost:8081/actuator/health

# Admin Console
curl http://localhost:3001/api/health

# PostgreSQL
docker exec authfusion-postgres pg_isready -U authfusion
```

### 9.2 Prometheus 메트릭 (SSO Server)

```bash
curl http://localhost:8081/actuator/prometheus
```

주요 메트릭:
- `authfusion_login_success_total` - 로그인 성공
- `authfusion_login_failure_total` - 로그인 실패
- `authfusion_token_issued_total` - 토큰 발급
- `authfusion_active_sessions` - 활성 세션

---

## 10. 백업 및 복구

```bash
# PostgreSQL 백업
pg_dump -U authfusion -h localhost authfusion_sso > backup_$(date +%Y%m%d).sql

# 복구
psql -U authfusion -h localhost authfusion_sso < backup_20260305.sql

# 설정 파일 백업
tar czf config-backup.tar.gz /opt/authfusion/config/
```

| 대상 | 백업 방법 | 주기 |
|------|----------|------|
| PostgreSQL | pg_dump | 매일 |
| 설정 파일 | 파일 복사 | 변경 시 |
| TLS 인증서 | 파일 복사 | 갱신 시 |
| Vault 데이터 | vault snapshot | 매일 |

---

## 11. 문제 해결

### SSO Server 시작 실패
```bash
# 로그 확인
docker compose logs sso-server
journalctl -u authfusion-sso -f

# 원인: DB 연결 실패 → SPRING_DATASOURCE_URL 확인
# 원인: 포트 충돌 → SERVER_PORT 변경
# 원인: 메모리 부족 → -Xmx 조정
```

### Admin Console API 호출 실패
```bash
# SSO Server 실행 확인
curl http://localhost:8081/actuator/health

# CORS 확인
# authfusion.sso.cors.allowed-origins에 Admin Console URL 포함
```

### DB 연결 실패
```bash
psql -h localhost -p 5432 -U authfusion -d authfusion_sso -c "SELECT 1"
```

### TLS 인증서 검증
```bash
openssl x509 -in server.crt -text -noout
openssl x509 -noout -modulus -in server.crt | openssl md5
openssl rsa -noout -modulus -in server.key | openssl md5
```

---

## 12. 빠른 참조

### 로컬 개발 (전체 스택)
```bash
docker compose up -d
# SSO Server → http://localhost:8081
# Admin Console → http://localhost:3001
```

### 로컬 개발 (개별 실행)
```bash
docker compose up -d postgres                              # DB
cd products/sso-server && mvn spring-boot:run               # SSO Server
cd products/admin-console && npm install && npm run dev     # Admin Console
cd products/sso-agent && mvn clean install                  # SSO Agent
```

### CC 모드 (Docker)
```bash
export AUTHFUSION_KEY_MASTER_SECRET=<마스터키>
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
```

### 운영 배포
```bash
# SSO Server
cd products/sso-server && mvn clean package -Pcc
sudo systemctl start authfusion-sso

# Admin Console
cd products/admin-console && npm ci && npm run build
sudo systemctl start authfusion-admin
```

---

## 13. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-03-03 | 초기 작성 |
| 1.1 | 2026-03-05 | 모노레포 구조 반영, 제품별 로컬/운영 실행 가이드 추가 |
