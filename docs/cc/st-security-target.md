# 보안목표명세서 (Security Target)

## AuthFusion SSO Server v1.0

---

## 1. 보안목표명세서 소개

### 1.1 ST 참조

- **ST 제목**: AuthFusion SSO Server 보안목표명세서
- **ST 버전**: 1.0
- **ST 작성일**: 2026-03-03
- **CC 버전**: Common Criteria v3.1 Revision 5
- **EAL**: EAL2+ (증강: ALC_FLR.1)

### 1.2 TOE 참조

- **TOE 명칭**: AuthFusion SSO Server
- **TOE 버전**: 1.0.0
- **TOE 개발사**: AuthFusion Team

### 1.3 문서 규약

본 문서는 CC(Common Criteria) Part 1, 2, 3에 정의된 용어와 표기법을 따른다.

---

## 2. TOE 개요

### 2.1 TOE 유형

AuthFusion SSO Server는 OIDC(OpenID Connect) 기반의 **통합 인증 서버(Identity and Access Management)** 제품이다.

### 2.2 TOE 주요 보안 기능

| 보안 기능 | 설명 |
|-----------|------|
| 사용자 식별 및 인증 | OIDC 프로토콜 기반 인증 (Authorization Code + PKCE) |
| 접근 제어 | RBAC 기반 역할별 접근 제어 정책 적용 |
| 암호키 관리 | RSA 키 페어 생성, 보관, 로테이션, 파기 |
| 암호 연산 | JWT 서명/검증(RS256), 비밀번호 해싱(BCrypt), 키 암호화(AES-256-GCM) |
| 감사 로그 | 보안 관련 이벤트 생성, 저장, 조회 |
| 세션 관리 | SSO 세션 생성, 유지, 타임아웃, 강제 종료 |
| 인증 실패 대응 | 무차별 대입 공격 방어 (계정 잠금, 요청 빈도 제한) |

### 2.3 TOE 구성 요소

```
┌─────────────────────────────────────────────────────────────┐
│                    TOE 경계 (TOE Boundary)                   │
│                                                              │
│  ┌──────────────────┐  ┌──────────────────┐                  │
│  │  SSO Server      │  │  SSO Agent       │                  │
│  │  (OIDC Provider) │  │  (Servlet Filter)│                  │
│  │                  │  │                  │                  │
│  │  - 인증/인가     │  │  - 토큰 검증     │                  │
│  │  - 토큰 관리     │  │  - 세션 연계     │                  │
│  │  - 세션 관리     │  │  - 요청 필터링   │                  │
│  │  - 감사 로그     │  │                  │                  │
│  │  - 키 관리       │  │                  │                  │
│  │  - 관리 콘솔     │  │                  │                  │
│  └──────────────────┘  └──────────────────┘                  │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────┴──────────┐
                    │  운영 환경 (비-TOE)  │
                    │                     │
                    │  - PostgreSQL DB    │
                    │  - HashiCorp Vault  │
                    │  - NTP Server       │
                    │  - SIEM             │
                    └─────────────────────┘
```

---

## 3. TOE 설명

### 3.1 물리적 범위

TOE의 물리적 범위는 다음과 같다:

- **SSO Server**: Spring Boot 3.2 기반 OIDC Provider 애플리케이션 (JAR 패키지)
- **SSO Agent**: Jakarta Servlet Filter 기반 인증 에이전트 라이브러리 (JAR 패키지)
- **관리 콘솔**: Next.js 14 기반 웹 관리 인터페이스

### 3.2 논리적 범위

TOE는 다음 보안 기능을 제공한다:

1. **식별 및 인증 (Identification and Authentication)**
2. **접근 제어 (Access Control)**
3. **암호 지원 (Cryptographic Support)**
4. **감사 (Audit)**
5. **세션 관리 (Session Management)**
6. **신뢰 경로/채널 (Trusted Path/Channel)**

### 3.3 운영 환경

| 구성 요소 | 역할 | TOE 여부 |
|-----------|------|----------|
| PostgreSQL 16 | 사용자/클라이언트/감사 데이터 저장 | 비-TOE |
| HashiCorp Vault | 마스터 키 보관 (선택) | 비-TOE |
| Thales Luna HSM | 하드웨어 키 보호 (선택) | 비-TOE |
| NTP Server | 시간 동기화 | 비-TOE |
| SIEM | 감사 로그 장기 보관/분석 | 비-TOE |
| 운영 체제 | Linux (RHEL/Ubuntu) | 비-TOE |
| JRE | Java 17 런타임 | 비-TOE |

---

## 4. 보안 환경

### 4.1 위협 (Threats)

| 위협 ID | 위협명 | 설명 |
|---------|--------|------|
| T.UNAUTH_ACCESS | 비인가 접근 | 공격자가 인증을 우회하여 보호 자원에 접근 |
| T.BRUTE_FORCE | 무차별 대입 공격 | 공격자가 반복적 로그인 시도로 자격증명 탈취 |
| T.TOKEN_FORGE | 토큰 위조 | 공격자가 유효한 JWT 토큰을 위조하여 인증 우회 |
| T.KEY_COMPROMISE | 키 유출 | 서명키 또는 암호키가 비인가 주체에게 노출 |
| T.SESSION_HIJACK | 세션 탈취 | 공격자가 유효한 세션을 가로채어 사용 |
| T.AUDIT_TAMPER | 감사 로그 변조 | 공격자가 감사 로그를 삭제 또는 변조 |
| T.REPLAY | 재전송 공격 | 공격자가 이전 인증 메시지를 재전송 |
| T.EAVESDROP | 도청 | 네트워크 통신 내용을 비인가 도청 |

### 4.2 조직 보안 정책 (Organizational Security Policies)

| 정책 ID | 정책명 | 설명 |
|---------|--------|------|
| P.AUDIT | 감사 정책 | 모든 보안 관련 이벤트는 감사 로그로 기록되어야 한다 |
| P.ACCESS_CONTROL | 접근 제어 정책 | RBAC 기반 최소 권한 원칙을 적용한다 |
| P.CRYPTO | 암호 정책 | 검증된 암호 알고리즘만 사용한다 (RSA-2048+, AES-256) |
| P.SESSION | 세션 정책 | 세션 타임아웃 및 동시 세션 제한을 적용한다 |
| P.PASSWORD | 비밀번호 정책 | 비밀번호 복잡도/길이/이력 관리를 수행한다 |

### 4.3 가정 (Assumptions)

| 가정 ID | 가정명 | 설명 |
|---------|--------|------|
| A.PHYSICAL | 물리적 보안 | TOE가 설치된 서버는 물리적으로 보호된 환경에 위치한다 |
| A.NETWORK | 네트워크 보안 | TOE와 사용자 간 통신은 TLS 1.2 이상으로 보호된다 |
| A.ADMIN | 관리자 신뢰 | TOE 관리자는 신뢰할 수 있고 적절히 교육받은 인원이다 |
| A.TIME | 시간 동기화 | TOE 운영 환경은 신뢰할 수 있는 NTP 서버와 동기화된다 |
| A.OS | 운영 체제 보안 | TOE가 실행되는 운영 체제는 적절히 보안 설정되어 있다 |
| A.DB | 데이터베이스 보안 | PostgreSQL DB는 접근 제어 및 암호화가 적용되어 있다 |

---

## 5. 보안 목적

### 5.1 TOE 보안 목적

| 목적 ID | 목적명 | 설명 |
|---------|--------|------|
| O.AUTH | 인증 | TOE는 사용자와 클라이언트를 OIDC 프로토콜로 인증한다 |
| O.ACCESS | 접근 제어 | TOE는 RBAC 기반 접근 제어를 시행한다 |
| O.CRYPTO | 암호 지원 | TOE는 검증된 암호 알고리즘으로 키 관리 및 암호 연산을 수행한다 |
| O.AUDIT | 감사 | TOE는 보안 이벤트를 감사 로그로 기록하고 조회 기능을 제공한다 |
| O.SESSION | 세션 관리 | TOE는 세션 타임아웃 및 강제 종료를 지원한다 |
| O.PROTECT | 자체 보호 | TOE는 자체 보안 메커니즘의 우회를 방지한다 |
| O.CHANNEL | 신뢰 채널 | TOE는 신뢰할 수 있는 통신 채널을 사용한다 |

### 5.2 운영 환경 보안 목적

| 목적 ID | 목적명 | 설명 |
|---------|--------|------|
| OE.PHYSICAL | 물리적 보호 | 운영 환경은 TOE의 물리적 보안을 보장한다 |
| OE.TLS | TLS 통신 | 운영 환경은 TLS 1.2+ 통신을 제공한다 |
| OE.TIME | 시간 동기화 | 운영 환경은 신뢰할 수 있는 시간원을 제공한다 |
| OE.DB | 데이터 보호 | 운영 환경은 데이터베이스 보안을 보장한다 |

---

## 6. IT 보안 요구사항

### 6.1 보안 기능 요구사항 (SFR)

#### 6.1.1 식별 및 인증 (FIA)

| SFR | 요구사항명 | 설명 |
|-----|-----------|------|
| **FIA_UAU.1** | 행위 전 인증 | TOE는 사용자를 식별된 행위를 수행하기 전에 인증한다 |
| **FIA_UID.1** | 행위 전 식별 | TOE는 사용자를 행위 전에 식별한다 |
| **FIA_AFL.1** | 인증 실패 처리 | TOE는 연속 인증 실패 시 계정을 잠금한다 (기본: 5회 실패 시 30분 잠금) |
| **FIA_USB.1** | 사용자-주체 바인딩 | TOE는 인증된 사용자의 보안 속성을 주체에 바인딩한다 |

#### 6.1.2 암호 지원 (FCS)

| SFR | 요구사항명 | 설명 |
|-----|-----------|------|
| **FCS_CKM.1** | 암호키 생성 | TOE는 RSA-2048 이상의 키 페어를 생성한다 |
| **FCS_CKM.2** | 암호키 분배 | TOE는 JWKS 엔드포인트를 통해 공개키를 안전하게 분배한다 |
| **FCS_CKM.4** | 암호키 파기 | TOE는 키 로테이션 시 이전 키를 안전하게 파기한다 |
| **FCS_COP.1** | 암호 연산 | TOE는 RS256(JWT 서명), BCrypt(비밀번호), AES-256-GCM(키 암호화) 연산을 수행한다 |

#### 6.1.3 접근 제어 (FDP)

| SFR | 요구사항명 | 설명 |
|-----|-----------|------|
| **FDP_ACC.1** | 부분 접근 제어 | TOE는 RBAC 정책에 따른 접근 제어를 시행한다 |
| **FDP_ACF.1** | 보안 속성 기반 접근 제어 | TOE는 사용자 역할, 스코프 등 보안 속성에 기반하여 접근을 제어한다 |

#### 6.1.4 감사 (FAU)

| SFR | 요구사항명 | 설명 |
|-----|-----------|------|
| **FAU_GEN.1** | 감사 데이터 생성 | TOE는 인증, 접근 제어, 관리 행위 등의 감사 데이터를 생성한다 |
| **FAU_GEN.2** | 사용자 신원 연관 | TOE는 감사 이벤트에 사용자 신원 정보를 연관시킨다 |
| **FAU_SAR.1** | 감사 검토 | TOE는 인가된 관리자에게 감사 로그 조회 기능을 제공한다 |

#### 6.1.5 세션 관리 (FTA)

| SFR | 요구사항명 | 설명 |
|-----|-----------|------|
| **FTA_SSL.3** | TSF 주도 세션 종료 | TOE는 세션 비활성 타임아웃 시 세션을 자동 종료한다 |

#### 6.1.6 신뢰 경로/채널 (FTP)

| SFR | 요구사항명 | 설명 |
|-----|-----------|------|
| **FTP_TRP.1** | 신뢰 경로 | TOE는 TLS를 통한 신뢰 경로를 제공한다 |

### 6.2 보안 보증 요구사항 (SAR)

| SAR | 요구사항명 | EAL2+ 해당 |
|-----|-----------|------------|
| ADV_ARC.1 | 보안 아키텍처 기술 | O |
| ADV_FSP.2 | 보안 시행 완전 기능 명세 | O |
| ADV_TDS.1 | 기본 설계 | O |
| AGD_OPE.1 | 운영 사용자 지침 | O |
| AGD_PRE.1 | 설치/설정 지침 | O |
| ALC_CMC.2 | CM 능력 활용 | O |
| ALC_CMS.2 | TOE CM 범위 | O |
| ALC_DEL.1 | 배포 절차 | O |
| ALC_FLR.1 | 기본 결함 교정 | O (증강) |
| ATE_COV.1 | 커버리지 증거 | O |
| ATE_FUN.1 | 기능 시험 | O |
| ATE_IND.2 | 독립 시험 | O |
| AVA_VAN.2 | 취약성 분석 | O |

---

## 7. TOE 요약 명세

### 7.1 보안 기능과 SFR 매핑

| 보안 기능 | 구현 메커니즘 | 관련 SFR |
|-----------|-------------|----------|
| OIDC 인증 | Authorization Code + PKCE 흐름 | FIA_UAU.1, FIA_UID.1 |
| 인증 실패 대응 | BruteForceProtectionService (5회/30분) | FIA_AFL.1 |
| 사용자-주체 바인딩 | SecurityContext에 인증 정보 바인딩 | FIA_USB.1 |
| RSA 키 생성 | KeyPairManager (RSA-2048+) | FCS_CKM.1 |
| 공개키 분배 | JWKS 엔드포인트 | FCS_CKM.2 |
| 키 파기 | 키 로테이션 시 안전 삭제 | FCS_CKM.4 |
| JWT 서명/검증 | RS256 알고리즘 | FCS_COP.1 |
| 비밀번호 해싱 | BCrypt (cost factor 12+) | FCS_COP.1 |
| 키 암호화 | AES-256-GCM | FCS_COP.1 |
| RBAC 접근 제어 | Spring Security + 역할 기반 정책 | FDP_ACC.1, FDP_ACF.1 |
| 감사 로그 생성 | AuditService 이벤트 기록 | FAU_GEN.1, FAU_GEN.2 |
| 감사 로그 조회 | AuditController REST API | FAU_SAR.1 |
| 세션 타임아웃 | SessionService 비활성 종료 | FTA_SSL.3 |
| TLS 통신 | HTTPS 강제 (CC 모드) | FTP_TRP.1 |

---

## 8. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
