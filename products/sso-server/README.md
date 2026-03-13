# AuthFusion SSO Server

OIDC Provider 기반 SSO 서버 (Authorization Code + PKCE, Client Credentials, Refresh Token)

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 (OpenJDK) | 런타임 |
| Spring Boot | 3.2.5 | 애플리케이션 프레임워크 |
| PostgreSQL | 16 | 데이터베이스 |
| Nimbus JOSE+JWT | 9.37+ | JWT/JWK 처리 |
| Flyway | 10.x | DB 마이그레이션 |
| Thymeleaf | 3.1.x | 로그인 페이지 SSR |
| ZXing | 3.5.3 | TOTP QR 코드 생성 |

---

## 1. 로컬 개발 환경 실행

### 1.1 사전 요구사항

```bash
# Java 17 확인
java -version    # openjdk 17.x.x

# Maven 3.9+ 확인
mvn -version     # Apache Maven 3.9.x

# Docker (PostgreSQL 실행용)
docker --version # 24.x 이상
```

### 1.2 PostgreSQL 실행

SSO Server는 PostgreSQL이 필수입니다. 로컬에서 가장 간편한 방법은 Docker를 사용하는 것입니다.

```bash
# PostgreSQL 컨테이너 실행
docker run -d \
  --name authfusion-postgres \
  -p 5432:5432 \
  -e POSTGRES_USER=authfusion \
  -e POSTGRES_PASSWORD=authfusion-secret \
  -e POSTGRES_DB=authfusion_db \
  postgres:16

# 연결 확인
docker exec -it authfusion-postgres psql -U authfusion -d authfusion_db -c "SELECT 1"
```

또는 프로젝트 루트에서 Docker Compose로 PostgreSQL만 실행:

```bash
# 프로젝트 루트에서
docker compose up -d postgres
```

### 1.3 SSO Server 실행 (Maven)

```bash

# Maven PATH 설정 (PowerShell 방식)
$env:PATH = "C:\apache-maven-3.9.3\bin;" + $env:PATH
# Maven PATH 설정 (cmd 방식)
set PATH=C:\apache-maven-3.9.3\bin;%PATH%  

# 프로젝트 루트 기준
cd products/sso-server

# 의존성 설치 및 컴파일
mvn clean compile

# 테스트 실행
mvn test

# 개발 모드 실행 (기본 프로파일)
mvn spring-boot:run

# 또는 JAR 빌드 후 실행
mvn clean package -DskipTests
java -jar target/authfusion-sso-server-1.0.0-SNAPSHOT.jar
```

### 1.4 실행 확인

```bash
# 헬스 체크
curl http://localhost:8081/actuator/health

# OIDC Discovery
curl http://localhost:8081/.well-known/openid-configuration

# JWKS 공개키
curl http://localhost:8081/.well-known/jwks.json

# Swagger UI (개발 모드에서만)
open http://localhost:8081/swagger-ui.html
```

### 1.5 개발 시 유용한 환경 변수

```bash
# 마스터 키 (키 암호화용, 기본값 제공됨)
export AUTHFUSION_KEY_MASTER_SECRET=my-dev-master-key

# 로그 레벨 변경
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.authfusion.sso=DEBUG"

# 포트 변경
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9090"
```

---

## 2. Docker로 실행

### 2.1 단독 Docker 이미지 빌드

```bash
cd products/sso-server

# 이미지 빌드
docker build -t authfusion/sso-server:dev .

# 실행 (PostgreSQL이 이미 실행 중인 경우)
docker run -d \
  --name authfusion-sso-server \
  -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/authfusion_db?currentSchema=sso \
  -e SPRING_DATASOURCE_USERNAME=authfusion \
  -e SPRING_DATASOURCE_PASSWORD=authfusion-secret \
  -e AUTHFUSION_SSO_ISSUER=http://localhost:8081 \
  authfusion/sso-server:dev
```

### 2.2 Docker Compose (전체 스택)

```bash
# 프로젝트 루트에서
docker compose up -d

# SSO Server 로그 확인
docker compose logs -f sso-server

# 서비스 상태
docker compose ps
```

### 2.3 CC 모드 Docker Compose

```bash
# CC 하드닝 프로파일로 실행
export AUTHFUSION_KEY_MASTER_SECRET=<운영용-마스터키>
docker compose -f docker-compose.yml -f docker-compose.cc.yml up -d
```

---

## 3. 운영 환경 (도메인) 배포

### 3.1 운영 아키텍처

```
                    ┌────────────────────────────┐
                    │     Nginx (Reverse Proxy)   │
                    │  sso.aines.kr:443 (TLS)  │
                    └──────────┬─────────────────┘
                               │
                    ┌──────────▼─────────────────┐
                    │   SSO Server (port 8081)    │
                    │  Spring Boot Application    │
                    └──────────┬─────────────────┘
                               │
              ┌────────────────┼──────────────────┐
              │                │                   │
     ┌────────▼──────┐ ┌──────▼────────┐ ┌───────▼───────┐
     │  PostgreSQL   │ │   Vault/HSM   │ │   LDAP (선택) │
     │  (port 5432)  │ │  (port 8200)  │ │  (port 389)   │
     └───────────────┘ └───────────────┘ └───────────────┘
```

### 3.2 115번 서버 배포 (POC)

```bash
# ① Windows에서 빌드
cd products/sso-server
mvn clean package -DskipTests

# ② jar 전송
scp target/authfusion-sso-server-1.0.0-SNAPSHOT.jar ju@192.168.0.115:~/authfusion/

# ③ 115번에서 실행
ssh ju@192.168.0.115
cd ~/authfusion
java -jar authfusion-sso-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod

# ④ 백그라운드 실행 (nohup)
nohup java -jar authfusion-sso-server-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod > sso-server.log 2>&1 &
```

### 3.3 Nginx 리버스 프록시 설정

```nginx
# /usr/local/openresty/nginx/conf/nginx.conf (sso.aines.kr server block 추가)

server {
    listen 443 ssl;
    server_name sso.aines.kr;
    server_tokens off;
    ssl_certificate /home/ju/nginx/_wildcard_.aines.kr_2025042584537.crt.pem;
    ssl_certificate_key /home/ju/nginx/_wildcard_.aines.kr_2025042584537.key.pem;

    # ── SSO Server (Spring Boot :8081) ──
    location /api/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /oauth2/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /.well-known/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /sso/ {
        proxy_pass http://127.0.0.1:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /css/ {
        proxy_pass http://127.0.0.1:8081;
    }
    location /js/ {
        proxy_pass http://127.0.0.1:8081;
    }

    location /actuator/ {
        proxy_pass http://127.0.0.1:8081;
    }

    location /swagger-ui {
        proxy_pass http://127.0.0.1:8081;
    }
    location /v3/api-docs {
        proxy_pass http://127.0.0.1:8081;
    }

    # ── Admin Console (Next.js :3001) ──
    location / {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 3.4 운영 환경 체크리스트

| 항목 | 설명 | 필수 |
|------|------|------|
| TLS 인증서 | 신뢰 CA 발급 인증서 | O |
| 마스터 키 | `AUTHFUSION_KEY_MASTER_SECRET` 고유값 설정 | O |
| DB SSL | PostgreSQL TLS 연결 (`sslmode=verify-full`) | O (CC) |
| issuer URL | `authfusion.sso.issuer=https://sso.aines.kr` | O |
| CORS | Admin Console 도메인만 허용 | O |
| NTP 동기화 | TOTP MFA 정확성 (30초 오차 이내) | O |
| 방화벽 | 8081 내부만, 443 외부 허용 | O |
| 로그 경로 | `/opt/authfusion/logs/` 디스크 용량 확보 | O |
| 백업 | PostgreSQL 일일 백업 (pg_dump) | O |

---

## 4. 주요 설정 프로파일

| 프로파일 | 용도 | 활성화 방법 |
|---------|------|------------|
| (기본) | 로컬 개발 | 별도 설정 없음 |
| `docker` | Docker Compose 내부 | `SPRING_PROFILES_ACTIVE=docker` |
| `cc` | CC 하드닝 모드 | `SPRING_PROFILES_ACTIVE=cc` |
| `prod` | 운영 환경 (115번) | `--spring.profiles.active=prod` |
| `test` | 테스트 | 테스트 실행 시 자동 |

### CC 모드 vs 기본 모드 주요 차이

| 설정 | 기본 모드 | CC 모드 |
|------|----------|---------|
| 로그인 시도 제한 | 5회 | 3회 |
| 계정 잠금 시간 | 30분 | 1시간 |
| 비밀번호 최소 길이 | 8자 | 12자 |
| 비밀번호 이력 | 5개 | 10개 |
| 세션 타임아웃 | 1시간 | 30분 |
| Refresh Token TTL | 24시간 | 12시간 |
| MFA 대기 시간 | 5분 | 3분 |
| Swagger UI | 활성 | 비활성 |
| Actuator | health, info, metrics | health만 |
| 확장 기능 | 활성 | **비활성** |

---

## 5. DB 마이그레이션

Flyway로 자동 관리됩니다. 서버 시작 시 자동 적용됩니다.

| 버전 | 내용 |
|------|------|
| V1 | 사용자 테이블 (`sso_users`) |
| V2 | OAuth2 클라이언트 (`sso_clients`) |
| V3 | 역할/RBAC (`sso_roles`, `sso_user_roles`) |
| V4 | 세션 관리 (`sso_sessions`) |
| V5 | 감사 로그 (`sso_audit_events`) |
| V6 | 동의 관리 (`sso_consents`) |
| V7 | 비밀번호 이력 (`sso_password_history`) |
| V8 | 서명키 (`sso_signing_keys`) - AES-256-GCM 암호화 |
| V9 | MFA (`sso_totp_secrets`, `sso_recovery_codes`, `sso_mfa_pending_sessions`) |
| V10 | LDAP 연동 (`user_source`, `external_id`, `ldap_synced_at` 컬럼) |

---

## 6. API 엔드포인트

### OIDC 표준
| Method | Path | 설명 |
|--------|------|------|
| GET | `/.well-known/openid-configuration` | OIDC Discovery |
| GET | `/.well-known/jwks.json` | JWKS 공개키 세트 |
| GET/POST | `/oauth2/authorize` | 인가 (Authorization Code + PKCE) |
| POST | `/oauth2/token` | 토큰 발급/갱신 |
| GET | `/oauth2/userinfo` | 사용자 정보 |
| POST | `/oauth2/revoke` | 토큰 폐기 |

### 관리 API (`/api/v1/`)
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/mfa/verify` | MFA 검증 |
| GET/POST | `/api/v1/users` | 사용자 관리 |
| GET/POST | `/api/v1/clients` | 클라이언트 관리 |
| GET/POST | `/api/v1/roles` | 역할 관리 |
| GET | `/api/v1/sessions` | 세션 조회 |
| GET | `/api/v1/audit/events` | 감사 로그 |
| POST/GET | `/api/v1/mfa/*` | MFA 설정/관리 |
| GET | `/api/v1/extensions` | 확장 모듈 (Extended 모드) |

---

## 7. 테스트

```bash
# 단위 테스트
mvn test

# 통합 테스트 (Testcontainers 필요 시)
mvn verify

# 특정 테스트만 실행
mvn test -Dtest=TotpServiceTest

# 커버리지 리포트 (Jacoco)
mvn test jacoco:report
# 결과: target/site/jacoco/index.html
```

---

## 8. 문제 해결

### 포트 충돌
```bash
# 8081 포트 사용 중인 프로세스 확인
lsof -i :8081  # macOS/Linux
netstat -ano | findstr :8081  # Windows
```

### DB 연결 실패
```bash
# PostgreSQL 접속 확인
psql -h localhost -p 5432 -U authfusion -d authfusion_db -c "SELECT 1"

# Docker PostgreSQL 로그
docker logs authfusion-postgres
```

### Flyway 마이그레이션 오류
```bash
# 마이그레이션 상태 확인
curl http://localhost:8081/actuator/flyway

# 마이그레이션 복구 (체크섬 불일치 시)
java -jar target/authfusion-sso-server-*.jar --spring.flyway.repair
```
