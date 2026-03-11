# AuthFusion Admin Console

AuthFusion SSO Server의 웹 기반 관리자 콘솔. Next.js 14 (App Router) 기반.

## 주요 기능

- **대시보드**: 인증 통계, 헬스체크, 최근 감사 이벤트
- **사용자 관리**: 생성/수정/삭제, 역할 할당, MFA 상태 확인
- **클라이언트 관리**: OAuth2 클라이언트 등록/수정, Redirect URI, Grant Type 관리
- **역할(RBAC) 관리**: 역할 생성/수정/삭제, 사용자별 역할 할당
- **감사 로그**: 이벤트 검색/필터, 통계 조회
- **시스템 설정**: 헬스체크 모니터링, LDAP 설정 표시, 확장 모듈 현황

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Next.js | 14.2 | React 프레임워크 (App Router) |
| React | 18.3 | UI 라이브러리 |
| TypeScript | 5.5 | 타입 안전성 |
| Tailwind CSS | 3.4 | 스타일링 |
| Headless UI | 2.1 | 접근성 있는 UI 컴포넌트 |
| Recharts | 2.12 | 차트 라이브러리 |
| Axios | 1.7 | HTTP 클라이언트 |
| NextAuth | 4.24 | SSO Server OIDC 인증 |

---

## 1. 로컬 개발 환경 실행

### 1.1 사전 요구사항

```bash
# Node.js 20.x 이상
node -v    # v20.x.x

# npm
npm -v     # 10.x.x
```

### 1.2 SSO Server 실행 (필수)

Admin Console은 SSO Server API에 의존합니다. 먼저 SSO Server가 실행 중이어야 합니다.

```bash
# 방법 1: Docker Compose로 SSO Server + PostgreSQL 실행
docker compose up -d postgres sso-server

# 방법 2: SSO Server 직접 실행
cd products/sso-server && mvn spring-boot:run
```

### 1.3 환경 변수 설정

```bash
cd products/admin-console

# 환경 변수 파일 생성
cp .env.local.example .env.local
```

`.env.local` 파일 편집:

```bash
# SSO Server URL (브라우저에서 접근하는 URL)
NEXT_PUBLIC_SSO_SERVER_URL=http://localhost:8080

# SSO Server URL (서버사이드 - Docker 내부 시 http://sso-server:8080)
SSO_SERVER_URL=http://localhost:8080

# NextAuth 설정
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-nextauth-secret-generate-with-openssl-rand-base64-32

# OIDC 클라이언트 (SSO Server에 등록된 값)
SSO_CLIENT_ID=admin-console
SSO_CLIENT_SECRET=your-client-secret

# 앱 정보
NEXT_PUBLIC_APP_NAME=AuthFusion Admin Console
NEXT_PUBLIC_APP_VERSION=1.0.0
```

### 1.4 실행

```bash
# 의존성 설치
npm install

# 개발 서버 실행 (Hot Reload)
npm run dev
```

브라우저에서 http://localhost:3000 접속.

### 1.5 개발 명령어

```bash
# 개발 서버 (Hot Reload)
npm run dev

# 타입 체크
npx tsc --noEmit

# 린트
npm run lint

# 프로덕션 빌드
npm run build

# 프로덕션 모드 로컬 실행
npm run build && npm start
```

---

## 2. Docker로 실행

### 2.1 단독 Docker 이미지 빌드

```bash
cd products/admin-console

# 이미지 빌드
docker build -t authfusion/admin-console:dev .

# 실행
docker run -d \
  --name authfusion-admin-console \
  -p 3000:3000 \
  -e NEXT_PUBLIC_SSO_SERVER_URL=http://localhost:8080 \
  -e SSO_SERVER_URL=http://host.docker.internal:8080 \
  -e SSO_CLIENT_ID=admin-console \
  -e SSO_CLIENT_SECRET=your-secret \
  -e NEXTAUTH_URL=http://localhost:3000 \
  -e NEXTAUTH_SECRET=your-nextauth-secret \
  authfusion/admin-console:dev
```

### 2.2 Docker Compose (전체 스택)

```bash
# 프로젝트 루트에서
docker compose up -d

# Admin Console 로그 확인
docker compose logs -f admin-console
```

> **참고**: CC 모드(`docker-compose.cc.yml`)에서는 Admin Console이 `replicas: 0`으로 설정되어 배포되지 않습니다. CC 평가 시 관리자 콘솔은 별도 보안 네트워크에서 직접 실행합니다.

---

## 3. 운영 환경 (도메인) 배포

### 3.1 운영 아키텍처

```
사용자 (브라우저)
    │
    ▼
┌─────────────────────────────┐
│  Nginx (Reverse Proxy)       │
│  admin.example.com:443 (TLS) │
└──────────┬──────────────────┘
           │
┌──────────▼──────────────────┐
│  Admin Console (port 3000)   │ ──→ SSO Server (sso.example.com)
│  Next.js Standalone          │     API 호출 + OIDC 인증
└──────────────────────────────┘
```

### 3.2 운영 환경 변수

```bash
# .env.production (또는 환경 변수로 직접 설정)

# 공개 URL (브라우저에서 SSO Server에 접근하는 URL)
NEXT_PUBLIC_SSO_SERVER_URL=https://sso.example.com

# 내부 URL (서버사이드 API 호출용 - 내부 네트워크)
SSO_SERVER_URL=http://sso-server.internal:8080

# NextAuth
NEXTAUTH_URL=https://admin.example.com
NEXTAUTH_SECRET=<openssl rand -base64 32로 생성>

# OIDC 클라이언트
SSO_CLIENT_ID=admin-console
SSO_CLIENT_SECRET=<SSO Server에서 발급한 시크릿>

# 앱 설정
NODE_ENV=production
NEXT_PUBLIC_APP_NAME=AuthFusion Admin Console
```

### 3.3 Docker 운영 배포

```bash
# 운영 이미지 빌드
cd products/admin-console
docker build -t authfusion/admin-console:1.0.0 .

# 실행
docker run -d \
  --name authfusion-admin-console \
  --restart unless-stopped \
  -p 3000:3000 \
  -e NEXT_PUBLIC_SSO_SERVER_URL=https://sso.example.com \
  -e SSO_SERVER_URL=http://sso-server.internal:8080 \
  -e SSO_CLIENT_ID=admin-console \
  -e SSO_CLIENT_SECRET=${SSO_CLIENT_SECRET} \
  -e NEXTAUTH_URL=https://admin.example.com \
  -e NEXTAUTH_SECRET=${NEXTAUTH_SECRET} \
  -e NODE_ENV=production \
  authfusion/admin-console:1.0.0
```

### 3.4 Node.js 직접 배포

```bash
# 1. 빌드
cd products/admin-console
npm ci --production=false
npm run build

# 2. standalone 디렉토리 배포
cp -r .next/standalone /opt/authfusion/admin-console/
cp -r .next/static /opt/authfusion/admin-console/.next/static
cp -r public /opt/authfusion/admin-console/public

# 3. 실행
cd /opt/authfusion/admin-console
NODE_ENV=production \
NEXT_PUBLIC_SSO_SERVER_URL=https://sso.example.com \
SSO_SERVER_URL=http://sso-server.internal:8080 \
NEXTAUTH_URL=https://admin.example.com \
NEXTAUTH_SECRET=<시크릿> \
SSO_CLIENT_ID=admin-console \
SSO_CLIENT_SECRET=<시크릿> \
node server.js
```

### 3.5 systemd 서비스 등록

```ini
# /etc/systemd/system/authfusion-admin.service
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
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable authfusion-admin
sudo systemctl start authfusion-admin
```

### 3.6 Nginx 리버스 프록시

```nginx
# /etc/nginx/conf.d/authfusion-admin.conf

upstream admin_console {
    server 127.0.0.1:3000;
}

server {
    listen 80;
    server_name admin.example.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name admin.example.com;

    ssl_certificate     /etc/nginx/ssl/admin.example.com.crt;
    ssl_certificate_key /etc/nginx/ssl/admin.example.com.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;

    # 보안 헤더
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-Frame-Options DENY always;

    location / {
        proxy_pass http://admin_console;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket (Next.js HMR - 개발 시에만)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 3.7 SSO Server 클라이언트 등록

Admin Console이 SSO Server에 OIDC 로그인하려면 클라이언트 등록이 필요합니다.

```bash
curl -X POST https://sso.example.com/api/v1/clients \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "admin-console",
    "clientName": "AuthFusion Admin Console",
    "redirectUris": ["https://admin.example.com/api/auth/callback/authfusion"],
    "grantTypes": ["authorization_code", "refresh_token"],
    "scopes": ["openid", "profile", "email", "roles"],
    "requirePkce": true
  }'
```

### 3.8 운영 체크리스트

| 항목 | 설명 | 필수 |
|------|------|------|
| TLS 인증서 | `admin.example.com` 신뢰 CA 발급 | O |
| NEXTAUTH_SECRET | `openssl rand -base64 32`로 생성 | O |
| SSO_CLIENT_SECRET | SSO Server에서 발급 | O |
| NEXTAUTH_URL | `https://admin.example.com` | O |
| NEXT_PUBLIC_SSO_SERVER_URL | `https://sso.example.com` | O |
| Redirect URI | SSO Server에 등록된 값과 일치 | O |
| NODE_ENV | `production` | O |

---

## 4. 환경 변수 전체 목록

| 변수명 | 설명 | 기본값 | 필수 |
|--------|------|--------|------|
| `NEXT_PUBLIC_SSO_SERVER_URL` | SSO Server URL (브라우저) | `http://localhost:8080` | O |
| `SSO_SERVER_URL` | SSO Server URL (서버사이드) | `http://localhost:8080` | O |
| `SSO_CLIENT_ID` | OIDC 클라이언트 ID | `admin-console` | O |
| `SSO_CLIENT_SECRET` | OIDC 클라이언트 시크릿 | - | O |
| `NEXTAUTH_URL` | NextAuth 콜백 URL | `http://localhost:3000` | O |
| `NEXTAUTH_SECRET` | NextAuth 암호화 키 | - | O |
| `NODE_ENV` | 실행 환경 | `development` | - |
| `NEXT_PUBLIC_APP_NAME` | 앱 표시 이름 | `AuthFusion Admin Console` | - |
| `NEXT_PUBLIC_APP_VERSION` | 앱 버전 | `1.0.0` | - |

---

## 5. 프로젝트 구조

```
products/admin-console/
├── src/
│   ├── app/                    # Next.js App Router 페이지
│   │   ├── layout.tsx          # 루트 레이아웃 (사이드바 포함)
│   │   ├── page.tsx            # 대시보드
│   │   ├── globals.css         # 전역 스타일 (Tailwind + 커스텀)
│   │   ├── login/page.tsx      # 로그인 (MFA 포함)
│   │   ├── users/              # 사용자 관리
│   │   ├── clients/            # 클라이언트 관리
│   │   ├── keys/               # 키 관리
│   │   ├── audit/              # 감사 로그
│   │   ├── policies/           # 정책 관리
│   │   └── settings/           # 시스템 설정
│   ├── components/             # 재사용 컴포넌트
│   └── lib/                    # 유틸리티 및 API
│       ├── api.ts              # SSO Server API 클라이언트
│       ├── types.ts            # TypeScript 타입 정의
│       └── auth.ts             # 인증 헬퍼
├── public/                     # 정적 파일
├── package.json
├── tsconfig.json
├── tailwind.config.ts
├── next.config.js
├── Dockerfile
└── .env.local.example
```

---

## 6. 문제 해결

### API 연결 실패
```bash
# SSO Server 실행 확인
curl http://localhost:8080/actuator/health

# CORS 오류 시 SSO Server 설정 확인
# authfusion.sso.cors.allowed-origins에 http://localhost:3000 포함 여부
```

### OIDC 로그인 실패
```
# Redirect URI 확인: SSO Server에 등록된 URI와 NEXTAUTH_URL 비교
# Client Secret 확인: SSO Server에 등록된 값과 SSO_CLIENT_SECRET 비교
```

### 빌드 오류
```bash
# Node.js 버전 확인
node -v  # v20.x.x 필요

# 캐시 정리 후 재빌드
rm -rf .next node_modules
npm install
npm run build
```
