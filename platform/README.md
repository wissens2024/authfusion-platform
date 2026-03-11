# Platform 확장 모듈 (비-TOE)

이 디렉토리는 TOE에 포함되지 않는 플랫폼 확장 모듈을 위한 공간입니다.

## 디렉토리 구조

```
platform/
├── keyhub/       # KeyHub - 크리덴셜 관리 (향후)
├── connect/      # Connect - 외부 IdP 연동 (향후)
└── automation/   # Automation - 프로비저닝 자동화 (향후)
```

## CC 모드와의 관계

- CC 모드(`-Pcc` 빌드)에서는 이 디렉토리의 모듈이 비활성화됩니다.
- `@ExtendedFeature` 및 `@ConditionalOnExtendedMode` 어노테이션으로 제어됩니다.
- Extension Layer SPI (`com.authfusion.sso.extension.spi`)를 통해 SSO Server와 연동됩니다.

## 개발 예정

| 모듈 | 설명 | SPI |
|------|------|-----|
| KeyHub | API 키/시크릿 관리 | `CredentialExtension` |
| Connect | 소셜/기업 IdP 연동 (Google, GitHub 등) | `IdentityProviderExtension` |
| Automation | SCIM 2.0 기반 사용자 프로비저닝 | `ProvisioningExtension` |
