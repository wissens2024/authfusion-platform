# 형상관리 범위 (ALC_CMS)

## AuthFusion SSO Server 형상관리 문서

---

## 1. 개요

본 문서는 AuthFusion SSO Server의 형상관리(Configuration Management) 범위, 빌드 절차, SBOM(Software Bill of Materials), 재현 빌드, 서명 검증 절차를 기술한다.

### 1.1 목적

- TOE 구성 항목의 식별 및 추적
- 빌드 재현성 보장
- 배포물 무결성 검증 절차 제공
- 공급망 보안 투명성 확보 (SBOM)

### 1.2 적용 범위

- SSO Server (Spring Boot JAR)
- SSO Agent (Java JAR 라이브러리)
- Admin Console (Next.js 빌드 결과물)
- 설정 파일 및 마이그레이션 스크립트

---

## 2. CM 범위 (Configuration Management Scope)

### 2.1 형상 항목 목록

| 형상 항목 ID | 항목명 | 유형 | 버전 관리 | 설명 |
|-------------|--------|------|----------|------|
| CI-SSO-SRC | SSO Server 소스코드 | 소스 | Git | Java 17, Spring Boot 3.2 |
| CI-SSO-JAR | SSO Server 실행 파일 | 산출물 | Maven | authfusion-sso-server.jar |
| CI-AGT-SRC | SSO Agent 소스코드 | 소스 | Git | Java 17, Servlet Filter |
| CI-AGT-JAR | SSO Agent 라이브러리 | 산출물 | Maven | authfusion-sso-agent.jar |
| CI-ADM-SRC | Admin Console 소스코드 | 소스 | Git | Next.js 14, TypeScript |
| CI-ADM-BLD | Admin Console 빌드 결과 | 산출물 | npm | .next 빌드 디렉터리 |
| CI-CFG-APP | 애플리케이션 설정 | 설정 | Git | application.yml, application-cc.yml |
| CI-CFG-MIG | DB 마이그레이션 스크립트 | 설정 | Git | Flyway SQL 파일 |
| CI-CFG-DCK | Docker Compose 설정 | 설정 | Git | docker-compose.yml |
| CI-DOC-CC | CC 평가 문서 | 문서 | Git | docs/cc/ 디렉터리 |
| CI-DOC-EXT | 확장 문서 | 문서 | Git | docs/extended/ 디렉터리 |
| CI-DOC-CMN | 공통 문서 | 문서 | Git | docs/common/ 디렉터리 |
| CI-TST-INT | 통합 테스트 | 테스트 | Git | src/test/ |
| CI-TST-SEC | 보안 테스트 | 테스트 | Git | 보안 기능 검증 테스트 |

### 2.2 형상 항목 명명 규칙

```
authfusion-{모듈}-{버전}.{확장자}

예시:
  authfusion-sso-server-1.0.0.jar
  authfusion-sso-agent-1.0.0.jar
  authfusion-admin-console-1.0.0.tar.gz
```

### 2.3 버전 관리 체계

버전 체계는 **Semantic Versioning 2.0**을 따른다:

```
{MAJOR}.{MINOR}.{PATCH}[-{PRE-RELEASE}][+{BUILD-METADATA}]

예시:
  1.0.0           - 초기 릴리스
  1.0.1           - 버그 수정
  1.1.0           - 기능 추가 (하위 호환)
  2.0.0           - 주요 변경 (하위 비호환)
  1.0.0-rc.1      - 릴리스 후보
  1.0.0+cc-eval   - CC 평가용 빌드
```

---

## 3. 형상관리 도구 및 절차

### 3.1 형상관리 도구

| 도구 | 용도 | 버전 |
|------|------|------|
| Git | 소스코드 버전 관리 | 2.40+ |
| GitHub/GitLab | 원격 저장소 및 코드 리뷰 | - |
| Maven | Java 빌드 및 의존성 관리 | 3.9+ |
| npm | Node.js 의존성 관리 | 10+ |
| Docker | 컨테이너 빌드 | 24+ |

### 3.2 브랜치 전략

```
main          ← 릴리스 브랜치 (서명된 태그)
  │
  ├── develop ← 개발 통합 브랜치
  │     │
  │     ├── feature/xxx  ← 기능 개발
  │     ├── bugfix/xxx   ← 버그 수정
  │     └── security/xxx ← 보안 패치
  │
  └── release/x.y.z ← 릴리스 준비
        │
        └── hotfix/xxx ← 긴급 수정
```

### 3.3 변경 관리 절차

1. **변경 요청**: 이슈 트래커에 변경 사항 등록
2. **브랜치 생성**: 변경 유형에 따른 브랜치 생성
3. **코드 작성**: Conventional Commits 규약 준수
4. **코드 리뷰**: 최소 1인 이상의 리뷰어 승인 필요
5. **자동 테스트**: CI/CD 파이프라인에서 자동 테스트 실행
6. **보안 검토**: 보안 관련 변경은 보안 리뷰어 승인 필요
7. **머지**: develop 브랜치에 머지
8. **릴리스**: main 브랜치에 머지 및 태그 생성

### 3.4 커밋 메시지 규약

```
<type>(<scope>): <subject>

<body>

<footer>

타입:
  feat:     새 기능
  fix:      버그 수정
  security: 보안 패치
  refactor: 리팩터링
  test:     테스트 추가/수정
  docs:     문서 변경
  chore:    빌드/도구 변경
```

---

## 4. 빌드 절차

### 4.1 SSO Server 빌드

```bash
# 빌드 환경 요구사항
# - JDK 17 (OpenJDK)
# - Maven 3.9+

# 클린 빌드
cd products/sso-server
mvn clean package -DskipTests=false

# 빌드 결과물
# target/authfusion-sso-server-{version}.jar
```

### 4.2 SSO Agent 빌드

```bash
cd products/sso-agent
mvn clean package -DskipTests=false

# 빌드 결과물
# target/authfusion-sso-agent-{version}.jar
```

### 4.3 Admin Console 빌드

```bash
cd products/admin-console
npm ci                    # 의존성 설치 (lock 파일 기반)
npm run build             # 프로덕션 빌드
npm run test              # 테스트 실행

# 빌드 결과물
# .next/ 디렉터리
```

### 4.4 Docker 이미지 빌드

```bash
# 전체 스택 빌드
docker compose build

# 개별 이미지 빌드
docker build -t authfusion/sso-server:${VERSION} ./products/sso-server
docker build -t authfusion/admin-console:${VERSION} ./products/admin-console
```

---

## 5. SBOM (Software Bill of Materials)

### 5.1 SBOM 생성

SBOM은 CycloneDX 형식으로 생성된다:

```bash
# SSO Server SBOM 생성
cd products/sso-server
mvn org.cyclonedx:cyclonedx-maven-plugin:makeBom

# 결과: target/bom.json (CycloneDX JSON)
# 결과: target/bom.xml  (CycloneDX XML)

# Admin Console SBOM 생성
cd products/admin-console
npx @cyclonedx/cyclonedx-npm --output-file bom.json
```

### 5.2 주요 의존성 목록

#### SSO Server 핵심 의존성

| 의존성 | 버전 | 라이선스 | 용도 |
|--------|------|---------|------|
| Spring Boot | 3.2.5 | Apache 2.0 | 애플리케이션 프레임워크 |
| Spring Security | 6.2.x | Apache 2.0 | 보안 프레임워크 |
| Nimbus JOSE+JWT | 9.37+ | Apache 2.0 | JWT/JWK 처리 |
| PostgreSQL JDBC | 42.7.x | BSD-2-Clause | DB 연결 |
| Flyway | 10.x | Apache 2.0 | DB 마이그레이션 |
| Lombok | 1.18.x | MIT | 코드 생성 |
| Thymeleaf | 3.1.x | Apache 2.0 | 서버 사이드 렌더링 |
| BCrypt | (Spring 내장) | Apache 2.0 | 비밀번호 해싱 |

#### SSO Agent 핵심 의존성

| 의존성 | 버전 | 라이선스 | 용도 |
|--------|------|---------|------|
| Jakarta Servlet API | 6.0+ | EPL 2.0 | 서블릿 필터 |
| Nimbus JOSE+JWT | 9.37+ | Apache 2.0 | JWT 검증 |
| Spring Boot AutoConfig | 3.2.x | Apache 2.0 | 자동 설정 |

#### Admin Console 핵심 의존성

| 의존성 | 버전 | 라이선스 | 용도 |
|--------|------|---------|------|
| Next.js | 14.x | MIT | 웹 프레임워크 |
| React | 18.x | MIT | UI 라이브러리 |
| TypeScript | 5.x | Apache 2.0 | 타입 시스템 |
| Tailwind CSS | 3.x | MIT | 스타일링 |

### 5.3 SBOM 검증

```bash
# SBOM 유효성 검증
npx @cyclonedx/cyclonedx-cli validate --input-file bom.json

# 취약점 스캔
grype sbom:bom.json --output table
```

---

## 6. 재현 빌드 (Reproducible Build)

### 6.1 재현 빌드 원칙

동일한 소스코드와 빌드 환경에서 항상 동일한 산출물이 생성되어야 한다.

### 6.2 재현 빌드 보장 메커니즘

| 항목 | 메커니즘 | 설명 |
|------|---------|------|
| 의존성 고정 | `pom.xml` 버전 고정, `package-lock.json` | 정확한 의존성 버전 보장 |
| 빌드 도구 고정 | Maven Wrapper (`mvnw`) | 빌드 도구 버전 일관성 |
| 타임스탬프 제거 | `project.build.outputTimestamp` 설정 | JAR 내 타임스탬프 고정 |
| 정렬 보장 | 파일 시스템 순서 독립 | 엔트리 순서 일관성 |
| 환경 격리 | Docker 빌드 컨테이너 | OS/환경 차이 제거 |

### 6.3 재현 빌드 설정

```xml
<!-- pom.xml -->
<properties>
    <!-- 재현 빌드를 위한 타임스탬프 고정 -->
    <project.build.outputTimestamp>2026-03-03T00:00:00Z</project.build.outputTimestamp>
</properties>
```

### 6.4 재현 빌드 검증

```bash
# 첫 번째 빌드
mvn clean package -DskipTests
sha256sum target/authfusion-sso-server-1.0.0.jar > build1.sha256

# 두 번째 빌드 (클린 빌드)
mvn clean package -DskipTests
sha256sum target/authfusion-sso-server-1.0.0.jar > build2.sha256

# 해시 비교
diff build1.sha256 build2.sha256
# 차이가 없어야 함
```

---

## 7. 서명 검증

### 7.1 배포물 서명

릴리스 산출물은 GPG 키로 서명된다:

```bash
# JAR 서명
gpg --detach-sign --armor authfusion-sso-server-1.0.0.jar
# 결과: authfusion-sso-server-1.0.0.jar.asc

# SHA-256 체크섬 생성
sha256sum authfusion-sso-server-1.0.0.jar > authfusion-sso-server-1.0.0.sha256

# 체크섬 파일 서명
gpg --detach-sign --armor authfusion-sso-server-1.0.0.sha256
```

### 7.2 서명 검증 절차

```bash
# 1. GPG 공개키 임포트
gpg --import authfusion-release-key.pub

# 2. JAR 서명 검증
gpg --verify authfusion-sso-server-1.0.0.jar.asc authfusion-sso-server-1.0.0.jar

# 3. SHA-256 체크섬 검증
sha256sum -c authfusion-sso-server-1.0.0.sha256
```

### 7.3 Docker 이미지 서명

```bash
# Cosign을 사용한 이미지 서명
cosign sign --key cosign.key authfusion/sso-server:1.0.0

# 서명 검증
cosign verify --key cosign.pub authfusion/sso-server:1.0.0
```

### 7.4 서명 키 관리

| 키 유형 | 용도 | 보관 위치 |
|---------|------|----------|
| GPG 릴리스 키 | JAR/체크섬 서명 | 오프라인 보관 (에어갭) |
| Cosign 키 | Docker 이미지 서명 | 보안 키 관리 시스템 |
| Maven 서명 키 | Maven Central 배포 | GPG 키링 |

---

## 8. 릴리스 절차

### 8.1 릴리스 체크리스트

| 단계 | 작업 | 담당 |
|------|------|------|
| 1 | 모든 테스트 통과 확인 | 개발팀 |
| 2 | 보안 취약점 스캔 통과 | 보안팀 |
| 3 | SBOM 생성 및 검증 | 빌드팀 |
| 4 | 재현 빌드 검증 | 빌드팀 |
| 5 | 릴리스 노트 작성 | 개발팀 |
| 6 | 배포물 서명 | 릴리스 관리자 |
| 7 | Git 태그 생성 (서명) | 릴리스 관리자 |
| 8 | 배포물 게시 | 릴리스 관리자 |
| 9 | CC 문서 업데이트 | 기술문서팀 |

### 8.2 릴리스 산출물 구성

```
authfusion-release-1.0.0/
├── authfusion-sso-server-1.0.0.jar         # SSO Server JAR
├── authfusion-sso-server-1.0.0.jar.asc     # GPG 서명
├── authfusion-sso-server-1.0.0.sha256      # SHA-256 체크섬
├── authfusion-sso-agent-1.0.0.jar          # SSO Agent JAR
├── authfusion-sso-agent-1.0.0.jar.asc      # GPG 서명
├── authfusion-sso-agent-1.0.0.sha256       # SHA-256 체크섬
├── bom-sso-server.json                     # SSO Server SBOM
├── bom-sso-agent.json                      # SSO Agent SBOM
├── bom-admin-console.json                  # Admin Console SBOM
├── RELEASE-NOTES.md                        # 릴리스 노트
└── VERIFICATION.md                         # 검증 절차 안내
```

---

## 9. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|-----------|
| 1.0 | 2026-03-03 | AuthFusion Team | 초기 작성 |
