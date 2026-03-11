# 보안 정책 (Security Policy)

## 지원 버전

| 버전 | 지원 상태 |
|------|----------|
| sso-v1.x (LTS) | 보안 패치 지원 |
| 최신 main | 활성 개발 |

## 취약점 보고

보안 취약점을 발견한 경우, **공개 이슈로 보고하지 마시고** 다음 절차를 따라주세요:

1. **이메일**: security@authfusion.io 로 보고
2. **포함 정보**:
   - 취약점 유형 및 심각도 (CVSS 점수 가능 시)
   - 재현 절차
   - 영향 받는 버전
   - 가능한 경우 수정 제안

## 응답 시간

| 심각도 | 초기 응답 | 패치 목표 |
|--------|----------|----------|
| Critical (CVSS 9.0+) | 24시간 | 72시간 |
| High (CVSS 7.0-8.9) | 48시간 | 2주 |
| Medium (CVSS 4.0-6.9) | 1주 | 다음 릴리즈 |
| Low (CVSS < 4.0) | 2주 | 다음 메이저 릴리즈 |

## 보안 설계 원칙

- **키 관리**: RSA Private Key는 AES-256-GCM으로 암호화 저장
- **비밀번호**: BCrypt 해시 저장, 비밀번호 정책 강제
- **MFA**: TOTP (RFC 6238) 지원, 복구 코드 BCrypt 해시 저장
- **LDAP**: 바인드 비밀번호 환경변수 주입, LDAP Injection 방지
- **감사 로그**: 모든 보안 이벤트 불변 기록
- **PKCE**: Authorization Code 플로우 필수
- **CC 모드**: 확장 기능 비활성화, 타임아웃 단축

## 의존성 보안

- OWASP Dependency-Check로 SCA (CVSS 7.0 이상 실패)
- SpotBugs + Find Security Bugs로 SAST
- 매주 월요일 자동 스캔 (CI/CD)
- SBOM (CycloneDX) 생성 및 배포

## CC 인증

본 제품은 CC (Common Criteria) EAL2+ 인증을 목표로 합니다.
TOE 경계 및 평가 구성에 대한 상세 정보는 `docs/cc/` 를 참조하세요.
