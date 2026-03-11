# 기여 가이드 (Contributing Guide)

## 개발 환경 설정

```bash
# 저장소 클론
git clone https://github.com/your-org/authfusion-platform.git
cd authfusion-platform

# 개발 환경 설정
bash tools/dev/setup.sh
```

### 사전 요구사항

- Java 17 (Temurin)
- Maven 3.9+
- Node.js 20 LTS
- Docker & Docker Compose

## 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 다음 메이저/마이너 개발 |
| `release/sso-1.x-lts` | TOE LTS (백포트만) |
| `feature/*` | 기능 개발 |
| `fix/*` | 버그 수정 |

## 커밋 규칙

Conventional Commits를 따릅니다:

```
<type>(<scope>): <description>

[body]

[footer]
```

### 타입

| 타입 | 설명 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 변경 |
| `refactor` | 리팩토링 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드/CI 변경 |
| `security` | 보안 패치 |

### 스코프

| 스코프 | 대상 |
|--------|------|
| `sso-server` | SSO Server |
| `sso-agent` | SSO Agent |
| `admin-console` | Admin Console |
| `ci` | CI/CD |
| `docs` | 문서 |

### 예시

```
feat(sso-server): add TOTP MFA support

Implement RFC 6238 TOTP with AES-256-GCM encrypted secret storage.
Includes QR code generation, recovery codes, and login flow changes.
```

## PR 프로세스

1. `feature/*` 또는 `fix/*` 브랜치 생성
2. 변경사항 구현 및 테스트
3. `main` 브랜치로 PR 생성
4. 최소 1인 코드 리뷰 승인
5. CI 통과 확인
6. 머지

### TOE 변경 시 추가 절차

`products/sso-server/src/` 또는 `products/sso-agent/src/` 변경 시:

1. CC 영향 분석 라벨 추가
2. `tools/cc/toe-diff.sh` 실행하여 변경 범위 확인
3. `tools/cc/config-linter.sh` 실행하여 CC 설정 검증

## 빌드 및 테스트

```bash
# SSO Server
cd products/sso-server && mvn clean test

# SSO Agent
cd products/sso-agent && mvn clean test

# Admin Console
cd products/admin-console && npm run lint && npm run build

# CC 빌드
cd products/sso-server && mvn clean package -Pcc
```

## 코드 스타일

- **Java**: Google Java Style Guide, Lombok 사용
- **TypeScript**: strict mode, path alias `@/*`
- **API**: RESTful, `/api/v1/` prefix
- **문서**: 한국어 (Korean)
