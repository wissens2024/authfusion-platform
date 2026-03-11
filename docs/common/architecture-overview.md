# 전체 아키텍처 개요

## AuthFusion Platform 아키텍처 문서

---

## 1. 개요

AuthFusion Platform은 자체 개발 OIDC Provider를 중심으로 한 **SSO(Single Sign-On)** 솔루션이다.
SSO Server와 선택형 SSO Agent로 구성되며, CC(Common Criteria) 평가를 효율적으로 획득할 수 있는
3층 구조를 채택하고 있다.

### 1.1 설계 목표

1. **OIDC 표준 준수**: OpenID Connect 1.0 프로토콜 완전 구현
2. **CC 평가 대응**: TOE 경계 최소화 및 보안 기능 집중
3. **운영 편의성**: Admin Console을 통한 직관적 관리
4. **확장성**: SSO Agent를 통한 레거시 시스템 연동
5. **보안성**: 암호키 관리, 감사 로그, 접근 제어 내장

---

## 2. 3층 구조 (3-Tier Architecture)

### 2.1 구조 다이어그램

```
┌─────────────────────────────────────────────────────────────────────┐
│                                                                      │
│                     제1층: TOE 핵심 (Core TOE)                        │
│                                                                      │
│   ┌─────────────────────────────┐  ┌──────────────────────────┐     │
│   │      SSO Server             │  │     Admin Console        │     │
│   │    (Spring Boot 3.2)        │  │     (Next.js 14)         │     │
│   │                             │  │                          │     │
│   │  ┌────────┐ ┌────────┐     │  │  ┌─────────┐ ┌────────┐ │     │
│   │  │ OIDC   │ │ Token  │     │  │  │Dashboard│ │사용자  │ │     │
│   │  │ Engine │ │ Mgmt   │     │  │  │         │ │관리    │ │     │
│   │  └────────┘ └────────┘     │  │  └─────────┘ └────────┘ │     │
│   │  ┌────────┐ ┌────────┐     │  │  ┌─────────┐ ┌────────┐ │     │
│   │  │Session │ │ Audit  │     │  │  │클라이언 │ │감사    │ │     │
│   │  │ Mgmt   │ │ Log    │     │  │  │트 관리  │ │로그    │ │     │
│   │  └────────┘ └────────┘     │  │  └─────────┘ └────────┘ │     │
│   │  ┌────────┐ ┌────────┐     │  │                          │     │
│   │  │Crypto  │ │ RBAC   │     │  │    SSO Server API 호출   │     │
│   │  │Key Mgmt│ │ Engine │     │  │    (직접 DB 접근 없음)    │     │
│   │  └────────┘ └────────┘     │  │                          │     │
│   │                             │  │                          │     │
│   │     Port: 8080 (HTTPS)     │  │     Port: 3000 (HTTPS)  │     │
│   └─────────────────────────────┘  └──────────────────────────┘     │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                   제2층: TOE 에이전트 (Agent TOE)                      │
│                                                                      │
│   ┌──────────────────────────────────────────────────────────┐      │
│   │                    SSO Agent (JAR)                        │      │
│   │               Jakarta Servlet Filter                      │      │
│   │                                                           │      │
│   │   ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │      │
│   │   │ SsoAuth      │  │ Token        │  │ Agent        │  │      │
│   │   │ Filter       │  │ Validator    │  │ Session Mgr  │  │      │
│   │   └──────────────┘  └──────────────┘  └──────────────┘  │      │
│   │                                                           │      │
│   │   레거시 애플리케이션 서블릿 컨테이너에 배포                  │      │
│   └──────────────────────────────────────────────────────────┘      │
│                                                                      │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                    제3층: 운영 환경 (Non-TOE)                          │
│                                                                      │
│   ┌────────────┐ ┌────────────┐ ┌──────────┐ ┌──────────┐          │
│   │ PostgreSQL │ │ HashiCorp  │ │   NTP    │ │   SIEM   │          │
│   │    16      │ │   Vault    │ │  Server  │ │          │          │
│   │            │ │            │ │          │ │          │          │
│   │ 사용자DB   │ │ 마스터키   │ │ 시간동기 │ │ 로그보관 │          │
│   │ 클라이언트 │ │ 보관       │ │ 화       │ │ /분석    │          │
│   │ 감사로그   │ │            │ │          │ │          │          │
│   │ 서명키     │ │            │ │          │ │          │          │
│   │            │ │            │ │          │ │          │          │
│   │ Port: 5432│ │ Port: 8200│ │ Port:123│ │          │          │
│   └────────────┘ └────────────┘ └──────────┘ └──────────┘          │
│                                                                      │
│   ┌────────────┐ ┌────────────┐                                     │
│   │   AD/LDAP  │ │ Thales     │                                     │
│   │ (선택)     │ │ Luna HSM   │                                     │
│   │            │ │ (선택)     │                                     │
│   │ 외부 사용자│ │ 하드웨어   │                                     │
│   │ 저장소     │ │ 키 보호    │                                     │
│   └────────────┘ └────────────┘                                     │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

### 2.2 TOE vs 비-TOE 구분

#### TOE 포함 구성 요소

| 구성 요소 | 모듈 | 근거 |
|-----------|------|------|
| SSO Server 런타임 | sso-server | OIDC Provider 핵심 보안 기능 |
| 관리자 웹 콘솔 | admin-console | 관리 인터페이스 (보안 설정 관리) |
| 감사 로그 모듈 | sso-server (audit) | FAU_GEN.1 구현 |
| 토큰/세션/정책 로직 | sso-server (oidc, session, security) | 핵심 보안 결정 로직 |
| SSO Agent | sso-agent | 레거시 앱 인증 연동 (선택) |

#### 비-TOE 구성 요소

| 구성 요소 | 근거 |
|-----------|------|
| PostgreSQL | 범용 DBMS, 별도 CC 평가 대상 아님 |
| HashiCorp Vault | 외부 키 관리 서비스 |
| Thales Luna HSM | 외부 하드웨어 보안 모듈 |
| NTP Server | 외부 시간 동기화 서비스 |
| SIEM | 외부 로그 관리/분석 시스템 |
| AD/LDAP | 외부 디렉터리 서비스 |

---

## 3. 구성 요소 상세

### 3.1 SSO Server (sso-server)

OIDC Provider로서 인증/인가의 핵심 역할을 수행한다.

#### 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 LTS | 런타임 |
| Spring Boot | 3.2.5 | 애플리케이션 프레임워크 |
| Spring Security | 6.2.x | 보안 프레임워크 |
| Nimbus JOSE+JWT | 9.37+ | JWT/JWK 처리 |
| PostgreSQL JDBC | 42.7.x | DB 연결 |
| Flyway | 10.x | DB 마이그레이션 |
| Thymeleaf | 3.1.x | 로그인 페이지 렌더링 |

#### 핵심 모듈

```
sso-server/
├── oidc/            # OIDC 프로토콜 구현
│   ├── endpoint/    # REST 엔드포인트
│   ├── grant/       # 그랜트 타입 핸들러
│   └── service/     # OIDC 비즈니스 로직
├── jwt/             # JWT 토큰 처리
├── user/            # 사용자 관리
├── session/         # 세션 관리
├── security/        # 보안 설정 및 필터
├── audit/           # 감사 로그
├── rbac/            # 역할 기반 접근 제어
├── client/          # OAuth2 클라이언트 관리
├── web/             # 웹 페이지 (로그인 등)
└── config/          # 애플리케이션 설정
```

### 3.2 SSO Agent (sso-agent)

레거시 애플리케이션에 SSO 기능을 연동하기 위한 Servlet Filter 기반 JAR 라이브러리이다.

#### 동작 원리

```
레거시 앱 요청
     │
     ▼
┌─────────────────────┐
│ SsoAuthenticationFilter │
│ (Servlet Filter)        │
├─────────────────────────┤
│ 1. 세션 쿠키 확인       │
│ 2. 토큰 유효성 검증     │
│ 3. 미인증 → SSO 리다이렉트│
│ 4. 인증됨 → 요청 전달   │
└─────────────────────────┘
     │
     ▼
레거시 앱 서블릿
```

### 3.3 Admin Console (admin-console)

Next.js 14 기반의 관리자 웹 인터페이스이다.

#### 아키텍처 원칙

- **API 기반**: SSO Server REST API를 통해서만 데이터 접근 (직접 DB 접근 없음)
- **SSO 인증**: Admin Console 자체도 SSO Server를 통해 인증
- **ADMIN 역할 필수**: ADMIN 역할이 할당된 사용자만 접근 가능

---

## 4. 통신 구조

### 4.1 구성 요소 간 통신

```
사용자 브라우저 ──── HTTPS ────→ SSO Server (8080)
      │                              │
      │                              ├── JDBC/TLS → PostgreSQL (5432)
      │                              ├── HTTPS → Vault (8200)
      │                              └── Syslog → SIEM
      │
      ├──── HTTPS ────→ Admin Console (3000)
      │                       │
      │                       └── HTTPS → SSO Server API
      │
      └──── HTTPS ────→ 레거시 앱 + SSO Agent
                              │
                              └── HTTPS → SSO Server API
```

### 4.2 프로토콜 및 포트

| 통신 경로 | 프로토콜 | 포트 | 인증 |
|----------|---------|------|------|
| 사용자 → SSO Server | HTTPS | 8080 | OIDC / Bearer Token |
| 사용자 → Admin Console | HTTPS | 3000 | SSO (OIDC) |
| 사용자 → 레거시 앱 | HTTPS | 앱별 | SSO Agent |
| Admin Console → SSO Server | HTTPS | 8080 | Bearer Token |
| SSO Agent → SSO Server | HTTPS | 8080 | Bearer Token |
| SSO Server → PostgreSQL | JDBC/TLS | 5432 | 사용자/비밀번호 |
| SSO Server → Vault | HTTPS | 8200 | Vault Token |

---

## 5. 데이터 흐름

### 5.1 인증 데이터 흐름

```
사용자 자격증명
     │
     ▼
SSO Server: AuthService
     │
     ├── UserService: 사용자 조회 (PostgreSQL)
     ├── PasswordHashService: 비밀번호 검증 (BCrypt)
     ├── BruteForceProtection: 잠금 상태 확인
     ├── SessionService: 세션 생성
     ├── JwtTokenProvider: 토큰 생성 (RSA 서명)
     └── AuditService: 감사 로그 기록
           │
           ▼
     JWT 토큰 (Access + Refresh + ID Token)
```

### 5.2 데이터 저장 구조

| 데이터 유형 | 저장소 | 암호화 | 보존 기간 |
|------------|--------|--------|----------|
| 사용자 계정 | PostgreSQL | 비밀번호: BCrypt | 영구 |
| OAuth2 클라이언트 | PostgreSQL | Client Secret: BCrypt | 영구 |
| 서명키 (개인키) | PostgreSQL | AES-256-GCM | 키 로테이션 주기 |
| 감사 로그 | PostgreSQL | HMAC 무결성 | 90일 (DB) + SIEM |
| SSO 세션 | In-Memory | - | 세션 타임아웃 |
| 마스터 키 | Vault/HSM | HSM 하드웨어 보호 | 영구 |

---

## 6. 보안 아키텍처 요약

### 6.1 심층 방어 계층

```
계층 1: 네트워크 (TLS 1.2+, CORS, Rate Limiting)
  │
  ▼
계층 2: 인증 (OIDC, PKCE, 계정 잠금)
  │
  ▼
계층 3: 인가 (RBAC, 스코프 기반 접근 제어)
  │
  ▼
계층 4: 데이터 보호 (암호화, 해싱, 키 관리)
  │
  ▼
계층 5: 감사 (이벤트 로깅, 무결성 보호)
```

### 6.2 보안 기능 매핑

| 보안 영역 | 구현 기술 | CC SFR |
|----------|----------|--------|
| 인증 | OIDC Authorization Code + PKCE | FIA_UAU.1, FIA_UID.1 |
| 인증 실패 대응 | 계정 잠금, Rate Limiting | FIA_AFL.1 |
| 접근 제어 | RBAC + Spring Security | FDP_ACC.1, FDP_ACF.1 |
| 암호키 관리 | RSA-2048, AES-256-GCM | FCS_CKM.1, FCS_COP.1 |
| 감사 | AuditService + PostgreSQL | FAU_GEN.1, FAU_SAR.1 |
| 세션 관리 | SessionService + In-Memory Store | FTA_SSL.3 |
| 통신 보안 | TLS 1.2+ | FTP_TRP.1 |

---

## 7. 배포 토폴로지

### 7.1 단일 노드 배포 (개발/테스트)

```
Docker Host
  ├── sso-server (8080)
  ├── admin-console (3000)
  ├── postgres (5432)
  └── vault (8200)
```

### 7.2 운영 환경 배포

```
DMZ
  └── Nginx Reverse Proxy (443)
        ├── → SSO Server (8080)
        └── → Admin Console (3000)

Internal Network
  ├── PostgreSQL Primary + Replica
  ├── HashiCorp Vault Cluster
  ├── NTP Server
  └── SIEM Collector
```

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
