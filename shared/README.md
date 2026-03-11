# Shared 공통 라이브러리

이 디렉토리는 여러 제품 모듈에서 공유하는 공통 라이브러리를 위한 공간입니다.

## 디렉토리 구조

```
shared/
├── crypto/     # 암호화 유틸리티 (향후)
├── logging/    # 공통 로깅 (향후)
├── config/     # 공통 설정 (향후)
├── utils/      # 유틸리티 (향후)
└── contracts/  # API 계약/스키마 (향후)
```

## 현재 상태

현재 공통 라이브러리는 각 제품 모듈 내에 포함되어 있습니다.
향후 공통 코드를 이 디렉토리로 분리할 예정입니다.

## 분리 후보

| 모듈 | 현재 위치 | 설명 |
|------|----------|------|
| KeyEncryptionService | sso-server | AES-256-GCM 암호화 |
| AuditService | sso-server | 감사 로깅 공통 |
| JWT Utilities | sso-server/sso-agent | JWT 처리 공통 |
