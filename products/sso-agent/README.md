# AuthFusion SSO Agent

Servlet Filter 기반 SSO 연동 JAR 라이브러리. 레거시 Java 웹 애플리케이션을 AuthFusion SSO Server와 연동합니다.

## 기술 스택

| 기술 | 버전 | 용도 |
|------|------|------|
| Java | 17 (OpenJDK) | 런타임 |
| Jakarta Servlet | 6.0+ | Servlet Filter |
| Nimbus JOSE+JWT | 9.37+ | JWT 검증 |
| Spring Boot | 3.2.5 | 자동 설정 (선택) |

---

## 1. 로컬 개발 환경

### 1.1 사전 요구사항

```bash
# Java 17 확인
java -version    # openjdk 17.x.x

# Maven 3.9+ 확인
mvn -version     # Apache Maven 3.9.x
```

### 1.2 빌드

```bash
# 프로젝트 루트 기준
cd products/sso-agent

# 클린 빌드
mvn clean package

# 테스트 포함 빌드
mvn clean package -DskipTests=false

# CC 프로파일 빌드 (SBOM + GPG 서명 + 재현 빌드)
mvn clean package -Pcc

# 빌드 결과
ls target/sso-agent-1.0.0-SNAPSHOT.jar
```

### 1.3 로컬 Maven 저장소에 설치

```bash
# 로컬 ~/.m2/repository에 설치
mvn clean install

# 다른 프로젝트에서 의존성으로 사용 가능
```

---

## 2. 프로젝트에 통합하기

### 2.1 Maven 의존성 추가

```xml
<dependency>
    <groupId>com.authfusion</groupId>
    <artifactId>sso-agent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2.2 Spring Boot 프로젝트 (자동 설정)

SSO Agent는 Spring Boot Auto-Configuration을 지원합니다. 의존성 추가 후 `application.yml`에 설정만 추가하면 됩니다.

```yaml
# application.yml
authfusion:
  sso-agent:
    sso-server-url: http://localhost:8081   # SSO Server 주소
    client-id: my-app                       # OAuth2 클라이언트 ID
    client-secret: my-secret                # OAuth2 클라이언트 시크릿
    callback-path: /sso/callback            # OIDC 콜백 경로
    logout-path: /sso/logout                # 로그아웃 경로
    scope: openid profile email roles       # 요청 스코프
    session-cookie-name: SSO_AGENT_SESSION  # 세션 쿠키명
    session-timeout: 3600                   # 세션 타임아웃 (초)
    require-https: false                    # HTTPS 강제 여부
    jwks-cache-duration: 3600              # JWKS 캐시 (초)
    excluded-paths:                         # SSO 인증 제외 경로
      - /health
      - /public/**
      - /static/**
    access-rules:                           # 경로별 접근 제어
      - pattern: /admin/**
        roles:
          - ADMIN
      - pattern: /api/**
        authenticated: true
```

### 2.3 일반 Servlet 프로젝트 (수동 설정)

`web.xml` 또는 프로그래밍 방식으로 필터를 등록합니다.

#### web.xml 방식

```xml
<!-- web.xml -->
<filter>
    <filter-name>ssoAgentFilter</filter-name>
    <filter-class>com.authfusion.agent.filter.SsoAuthenticationFilter</filter-class>
    <init-param>
        <param-name>ssoServerUrl</param-name>
        <param-value>http://localhost:8081</param-value>
    </init-param>
    <init-param>
        <param-name>clientId</param-name>
        <param-value>my-app</param-value>
    </init-param>
    <init-param>
        <param-name>clientSecret</param-name>
        <param-value>my-secret</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>ssoAgentFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

#### 프로그래밍 방식

```java
@WebListener
public class SsoAgentInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        SsoAgentProperties props = new SsoAgentProperties();
        props.setSsoServerUrl("http://localhost:8081");
        props.setClientId("my-app");
        props.setClientSecret("my-secret");

        FilterRegistration.Dynamic filter = sce.getServletContext()
            .addFilter("ssoAgent", new SsoAuthenticationFilter(props));
        filter.addMappingForUrlPatterns(null, true, "/*");
    }
}
```

---

## 3. 인증 플로우

```
사용자 요청 → SsoAuthenticationFilter
                    │
                    ├── 세션 유효? → YES → 요청 통과 (SecurityContext 설정)
                    │
                    └── NO → SSO Server /oauth2/authorize 리다이렉트
                              │
                              ▼
                     SSO Server 로그인 (+ MFA)
                              │
                              ▼
                     /sso/callback?code=xxx&state=yyy
                              │
                              ▼
                     SSO Agent가 /oauth2/token 호출 → Access Token + ID Token 획득
                              │
                              ▼
                     세션 생성 → 원래 요청 URL로 리다이렉트
```

---

## 4. 운영 환경 (도메인) 배포

### 4.1 운영 설정

```yaml
# application.yml (운영 환경)
authfusion:
  sso-agent:
    sso-server-url: https://sso.aines.kr     # 운영 SSO Server 도메인
    client-id: my-production-app
    client-secret: ${SSO_CLIENT_SECRET}          # 환경 변수에서 주입
    callback-path: /sso/callback
    scope: openid profile email roles
    require-https: true                          # 운영에서는 HTTPS 강제
    session-timeout: 1800                        # 30분 (CC 모드 권장)
    jwks-cache-duration: 600                     # 10분
    excluded-paths:
      - /health
      - /public/**
```

### 4.2 SSO Server에 클라이언트 등록

SSO Agent를 사용하려면 SSO Server에 OAuth2 클라이언트를 먼저 등록해야 합니다.

```bash
# SSO Server API로 클라이언트 등록
curl -X POST https://sso.aines.kr/api/v1/clients \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "my-production-app",
    "clientName": "My Production App",
    "redirectUris": ["https://myapp.example.com/sso/callback"],
    "grantTypes": ["authorization_code", "refresh_token"],
    "scopes": ["openid", "profile", "email", "roles"],
    "requirePkce": true
  }'
```

### 4.3 운영 체크리스트

| 항목 | 설명 |
|------|------|
| SSO Server URL | `https://` 도메인 사용 |
| Client Secret | 환경 변수로 주입 (코드에 하드코딩 금지) |
| HTTPS 강제 | `require-https: true` |
| 세션 타임아웃 | CC 모드: 1800초 이하 권장 |
| 콜백 URL | SSO Server에 등록된 Redirect URI와 정확히 일치 |
| JWKS 캐시 | 키 교체 주기보다 짧게 설정 |
| 제외 경로 | 헬스체크, 정적 리소스만 제외 |

---

## 5. SecurityContext 활용

SSO Agent가 인증을 완료하면 `SsoSecurityContext`를 통해 사용자 정보에 접근할 수 있습니다.

```java
// Servlet에서 사용자 정보 접근
@WebServlet("/api/profile")
public class ProfileServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        SsoSecurityContext ctx = SsoSecurityContext.from(req);

        String username = ctx.getUsername();
        String email = ctx.getEmail();
        Set<String> roles = ctx.getRoles();

        if (ctx.hasRole("ADMIN")) {
            // 관리자 로직
        }
    }
}
```

---

## 6. 테스트

```bash
# 단위 테스트
mvn test

# 특정 테스트 실행
mvn test -Dtest=SsoAuthenticationFilterTest

# 전체 빌드 + 테스트
mvn clean verify
```

### 통합 테스트 (SSO Server 연동)

```bash
# 1. SSO Server 실행
cd products/sso-server && mvn spring-boot:run &

# 2. 테스트 앱에 SSO Agent 적용 후 실행
cd my-test-app && mvn spring-boot:run

# 3. 브라우저에서 http://localhost:8081 접속
#    → SSO Server 로그인 페이지로 리다이렉트
#    → 로그인 후 원래 페이지로 돌아옴
```

---

## 7. 문제 해결

### JWKS 가져오기 실패
```
SSO Server URL 확인: curl https://sso.aines.kr/.well-known/jwks.json
네트워크/방화벽: SSO Agent → SSO Server 8080 포트 연결 확인
```

### 콜백 오류
```
Redirect URI 불일치: SSO Server에 등록된 URI와 Agent의 callback-path 비교
HTTPS/HTTP: 운영에서는 HTTPS 콜백만 허용
```

### 세션 유지 안 됨
```
쿠키 설정: SameSite, Secure, Domain 속성 확인
로드밸런서: Sticky Session 또는 세션 공유 설정 필요
```
