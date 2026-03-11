# 업그레이드 및 롤백 가이드 (Upgrade & Rollback Guide)

## AuthFusion SSO Server v1.0

---

## 1. 개요

본 문서는 AuthFusion SSO Server 및 SSO Agent의 버전 업그레이드, 데이터베이스 마이그레이션,
롤백 절차를 상세히 설명한다. CC(Common Criteria) 평가 구성 환경에서의 업그레이드 제약 사항과
폐쇄망 환경에서의 업그레이드 절차도 포함한다.

### 1.1 대상 독자

- SSO Server 운영 담당자
- 보안 관리자
- 인프라/DevOps 엔지니어
- CC 평가 관련 담당자

### 1.2 전제 조건

- 현재 AuthFusion SSO Server가 정상 운영 중
- PostgreSQL 16+ 데이터베이스 접근 가능
- 데이터베이스 백업/복원 권한 보유
- 롤백 시나리오를 사전에 검증 완료

### 1.3 관련 문서

| 문서 | 경로 |
|------|------|
| 하드닝 가이드 | `docs/ops/hardening-guide.md` |
| 폐쇄망 설치 가이드 | `docs/ops/airgap-install.md` |
| 평가 구성 문서 | `docs/cc/evaluated-configuration.md` |
| TOE 경계 정의 | `docs/cc/toe-boundary.md` |

---

## 2. 버전 체계

### 2.1 시맨틱 버전 (Semantic Versioning)

```
sso-v{major}.{minor}.{patch}[-{pre-release}]
```

| 변경 유형 | 설명 | 예시 | DB 마이그레이션 |
|----------|------|------|----------------|
| **Major** | 호환성이 깨지는 변경, API 변경 | `sso-v2.0.0` | 거의 항상 필요 |
| **Minor** | 새 기능 추가, 하위 호환 유지 | `sso-v1.1.0` | 가능성 있음 |
| **Patch** | 버그 수정, 보안 패치 | `sso-v1.0.1` | 일반적으로 없음 |
| **Pre-release** | 릴리즈 후보 (CC 인증 전) | `sso-v1.1.0-rc1` | 가능성 있음 |

### 2.2 SSO Agent 버전 연동

SSO Agent는 SSO Server와 독립적 버전 체계를 사용하지만, 호환성 매트릭스를 반드시 확인한다.

| Agent 버전 | 호환 Server 버전 | 비고 |
|-----------|-----------------|------|
| agent-v1.0.x | sso-v1.0.x ~ v1.2.x | JWKS 공개키 검증 호환 |
| agent-v1.1.x | sso-v1.1.x ~ v1.3.x | 세션 동기화 API 추가 |

### 2.3 CC 평가 버전 관리

CC 평가 완료 버전은 별도 태그로 관리된다.

```
sso-v1.0.0-cc-certified    # CC 인증 통과 버전
sso-v1.0.1-cc-patch        # CC 인증 범위 내 보안 패치
```

> **주의**: CC 인증 버전의 변경은 평가기관과 사전 협의가 필요하다.
> ALC_FLR.1(기본 결함 교정)에 따라 보안 패치는 허용되나,
> 기능 추가는 재평가 대상이 될 수 있다.

---

## 3. 업그레이드 전 체크리스트

### 3.1 일반 체크리스트

```
[ ] 릴리즈 노트 확인 (CHANGELOG.md)
[ ] 대상 버전의 SBOM(Software Bill of Materials) 검토
[ ] SHA-256 체크섬 검증
[ ] GPG 서명 검증 (CC 빌드의 경우)
[ ] PostgreSQL 데이터베이스 전체 백업 완료
[ ] Flyway 마이그레이션 스크립트 내용 사전 검토
[ ] 롤백 SQL 스크립트 준비 완료
[ ] 현재 운영 설정(application.yml) 백업
[ ] 환경변수 목록 문서화
[ ] 테스트 환경에서 업그레이드 사전 검증 완료
[ ] 유지보수 윈도우 일정 확정 및 공지
[ ] 롤백 판단 기준 합의 (임계값, 제한 시간)
```

### 3.2 CC 모드 추가 체크리스트

```
[ ] CC 인증 범위 내 변경인지 확인
[ ] 평가 구성 유지 여부 검증 (evaluated-configuration.md 참조)
[ ] @ToeScope / @ExtendedFeature 어노테이션 변경 사항 검토
[ ] application-cc.yml 하드닝 설정 유지 확인
[ ] AUTHFUSION_SSO_CC_EXTENDED_FEATURES_ENABLED=false 유지
[ ] 확장 컨트롤러 비활성화 상태 유지 확인
[ ] 감사 로그 연속성 보장 방안 확인
```

### 3.3 폐쇄망 추가 체크리스트

```
[ ] 에어갭 업그레이드 번들 생성 완료 (인터넷 연결 환경에서)
[ ] 매체(USB/DVD)에 번들 복사 완료
[ ] 매체 반입 승인 획득
[ ] GPG 공개키 사전 반입 확인
[ ] 체크섬 파일 별도 경로(인쇄물 등)로 전달
```

---

## 4. 아티팩트 검증

### 4.1 SHA-256 체크섬 검증

```bash
# 체크섬 파일과 비교
sha256sum -c sso-server-1.1.0.sha256

# 수동 검증
sha256sum sso-server-1.1.0.jar
# 출력값과 릴리즈 노트의 체크섬 대조
```

### 4.2 GPG 서명 검증 (CC 빌드)

```bash
# GPG 공개키 임포트 (최초 1회)
gpg --import authfusion-release-key.asc

# 서명 검증
gpg --verify sso-server-1.1.0.jar.asc sso-server-1.1.0.jar
# "Good signature from ..." 메시지 확인
```

### 4.3 SBOM 변경 확인

```bash
# CycloneDX SBOM 비교 (이전 버전 vs 신규 버전)
diff <(jq -S . sbom-1.0.0.json) <(jq -S . sbom-1.1.0.json)

# 또는 CycloneDX CLI 사용
cyclonedx diff sbom-1.0.0.json sbom-1.1.0.json
```

---

## 5. 데이터베이스 백업

### 5.1 PostgreSQL 전체 백업

```bash
# 타임스탬프 포함 백업 파일명 생성
BACKUP_FILE="authfusion_backup_$(date +%Y%m%d_%H%M%S).sql"

# 전체 백업 (스키마 + 데이터)
pg_dump -U authfusion -h localhost -p 5432 \
  --format=custom \
  --blobs \
  --verbose \
  authfusion_sso > ${BACKUP_FILE}

# 백업 파일 무결성 확인
pg_restore --list ${BACKUP_FILE} > /dev/null 2>&1
echo "Exit code: $? (0이면 정상)"

# 백업 파일 크기 확인
ls -lh ${BACKUP_FILE}
```

### 5.2 Docker 환경 백업

```bash
# Docker Compose 환경에서 백업
docker exec authfusion-postgres pg_dump \
  -U authfusion \
  --format=custom \
  --blobs \
  authfusion_sso > authfusion_backup_$(date +%Y%m%d_%H%M%S).sql

# 볼륨 백업 (선택적 - 물리 백업)
docker run --rm \
  -v authfusion-postgres-data:/source:ro \
  -v $(pwd)/backup:/backup \
  alpine tar czf /backup/postgres-volume-$(date +%Y%m%d).tar.gz -C /source .
```

### 5.3 핵심 테이블 선택적 백업

민감 데이터를 포함하는 핵심 테이블의 별도 백업을 권장한다.

```bash
# 서명 키 테이블 (암호화된 비밀키 포함)
pg_dump -U authfusion -t sso_signing_keys authfusion_sso > signing_keys_backup.sql

# TOTP 비밀키 테이블 (암호화된 비밀키 포함)
pg_dump -U authfusion -t sso_totp_secrets authfusion_sso > totp_secrets_backup.sql

# 감사 로그 (롤백 시에도 보존 필요)
pg_dump -U authfusion -t sso_audit_events authfusion_sso > audit_events_backup.sql
```

> **중요**: `sso_signing_keys` 테이블의 `encrypted_private_key`와 `iv` 컬럼은
> AES-256-GCM으로 암호화되어 있으며, 복호화에는 `AUTHFUSION_KEY_MASTER_SECRET`
> 환경변수가 필요하다. 마스터 시크릿이 변경되면 기존 암호화된 키는 복호화 불가능하므로
> 마스터 시크릿도 안전하게 별도 백업한다.

---

## 6. 업그레이드 절차

### 6.1 JAR 배포 (systemd 단독 실행)

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  사전 검증   │───>│  서비스 중지 │───>│  JAR 교체   │───>│  서비스 시작 │
│  아티팩트    │    │  백업 수행   │    │  설정 갱신   │    │  헬스체크    │
│  검증       │    │             │    │             │    │  검증       │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

#### 단계 1: 아티팩트 다운로드 및 검증

```bash
# 아티팩트 디렉터리
RELEASE_DIR=/opt/authfusion/releases/v1.1.0
mkdir -p ${RELEASE_DIR}

# 아티팩트 배치 (사전에 다운로드 또는 전달)
# sso-server-1.1.0.jar, sso-server-1.1.0.sha256, sso-server-1.1.0.jar.asc

# 체크섬 검증
cd ${RELEASE_DIR}
sha256sum -c sso-server-1.1.0.sha256

# GPG 서명 검증 (CC 빌드)
gpg --verify sso-server-1.1.0.jar.asc sso-server-1.1.0.jar
```

#### 단계 2: 서비스 중지 및 백업

```bash
# 현재 상태 확인
systemctl status authfusion-sso

# 서비스 중지
systemctl stop authfusion-sso

# 서비스가 완전히 중지되었는지 확인
while systemctl is-active authfusion-sso > /dev/null 2>&1; do
  echo "서비스 중지 대기 중..."
  sleep 2
done
echo "서비스 중지 완료."

# JAR 백업
cp /opt/authfusion/sso-server.jar /opt/authfusion/sso-server.jar.bak.$(date +%Y%m%d)

# 설정 파일 백업
cp -r /opt/authfusion/config /opt/authfusion/config.bak.$(date +%Y%m%d)

# 데이터베이스 백업 (5절 참조)
pg_dump -U authfusion --format=custom authfusion_sso \
  > /opt/authfusion/backup/db_$(date +%Y%m%d_%H%M%S).sql
```

#### 단계 3: JAR 교체 및 설정 갱신

```bash
# 신규 JAR 배치
cp ${RELEASE_DIR}/sso-server-1.1.0.jar /opt/authfusion/sso-server.jar

# 설정 파일 갱신 (필요 시)
# 릴리즈 노트에 명시된 설정 변경 사항을 수동 반영
# diff를 사용하여 변경 내용 확인
diff /opt/authfusion/config/application.yml ${RELEASE_DIR}/application.yml.sample
```

#### 단계 4: 서비스 시작 및 검증

```bash
# 서비스 시작 (Flyway 마이그레이션 자동 실행)
systemctl start authfusion-sso

# 시작 로그 모니터링
journalctl -u authfusion-sso -f --since "1 minute ago"

# 헬스체크 (최대 90초 대기)
for i in $(seq 1 18); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "헬스체크 성공!"
    break
  fi
  echo "헬스체크 대기 중... (${i}/18)"
  sleep 5
done

# 상세 헬스체크
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Flyway 마이그레이션 확인
curl -s http://localhost:8080/actuator/flyway | python3 -m json.tool

# OIDC Discovery 엔드포인트 검증
curl -s http://localhost:8080/.well-known/openid-configuration | python3 -m json.tool

# JWKS 엔드포인트 검증
curl -s http://localhost:8080/.well-known/jwks.json | python3 -m json.tool
```

### 6.2 Docker Compose 배포

#### 단계 1: 사전 준비

```bash
# 현재 이미지 태그 확인
docker compose images

# 데이터베이스 백업
docker exec authfusion-postgres pg_dump \
  -U authfusion \
  --format=custom \
  authfusion_sso > backup_$(date +%Y%m%d_%H%M%S).sql

# 현재 docker-compose.yml 백업
cp docker-compose.yml docker-compose.yml.bak.$(date +%Y%m%d)
```

#### 단계 2: 이미지 업데이트

```bash
# 방법 A: 레지스트리에서 Pull (온라인 환경)
docker compose pull sso-server

# 방법 B: 로컬 빌드 (소스 코드 업데이트 후)
cd products/sso-server
git checkout sso-v1.1.0
docker compose build sso-server

# 방법 C: 에어갭 환경 (사전 export된 이미지 로드)
docker load -i authfusion-sso-server-1.1.0.tar
```

#### 단계 3: 서비스 업데이트

```bash
# SSO Server만 업데이트 (다른 서비스는 영향 없음)
docker compose up -d --no-deps sso-server

# 로그 모니터링
docker compose logs -f sso-server

# 헬스체크
docker compose exec sso-server \
  wget -qO- http://localhost:8080/actuator/health

# OIDC Discovery 검증
docker compose exec sso-server \
  wget -qO- http://localhost:8080/.well-known/openid-configuration
```

#### 단계 4: CC 모드 Docker Compose 업데이트

CC 모드 환경에서는 `docker-compose.cc.yml` 오버라이드를 함께 사용한다.

```bash
# CC 모드 업데이트
docker compose \
  -f docker-compose.yml \
  -f docker-compose.cc.yml \
  up -d --no-deps sso-server

# CC 모드 설정 확인
docker compose exec sso-server \
  wget -qO- http://localhost:8080/actuator/health

# Swagger UI 비활성화 확인 (404 반환 정상)
curl -sf http://localhost:8080/swagger-ui.html || echo "Swagger UI 비활성화 정상"
```

### 6.3 SSO Agent 업그레이드

SSO Agent는 레거시 애플리케이션에 Servlet Filter JAR로 임베드되므로, 각 애플리케이션별로 업그레이드한다.

```bash
# 1. Agent JAR 검증
sha256sum -c sso-agent-1.1.0.sha256

# 2. 레거시 앱 중지
systemctl stop legacy-app

# 3. 기존 Agent JAR 백업
cp /opt/legacy-app/lib/sso-agent-1.0.0.jar \
   /opt/legacy-app/lib/sso-agent-1.0.0.jar.bak

# 4. 신규 Agent JAR 배치
cp sso-agent-1.1.0.jar /opt/legacy-app/lib/sso-agent.jar

# 5. Agent 설정 갱신 (필요 시)
# sso-agent.yml 또는 application.yml 내 agent 설정 확인

# 6. 레거시 앱 시작
systemctl start legacy-app

# 7. Agent 동작 검증 - SSO Server JWKS 통신 확인
curl -s http://legacy-app:8081/health
```

> **주의**: SSO Agent는 SSO Server의 JWKS 엔드포인트를 통해 공개키를 가져와
> JWT를 검증한다. Agent 업그레이드 전에 반드시 SSO Server가 먼저 업그레이드되어야 한다.

---

## 7. 데이터베이스 마이그레이션 관리

### 7.1 Flyway 마이그레이션 개요

AuthFusion SSO Server는 Flyway를 사용하여 데이터베이스 스키마를 관리한다.
서비스 시작 시 자동으로 pending 마이그레이션이 실행된다.

```
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.baseline-on-migrate=true
```

### 7.2 현재 마이그레이션 이력

```
db/migration/
├── V1__create_users_table.sql                  # 사용자 테이블
├── V2__create_password_history_table.sql        # 비밀번호 이력
├── V3__create_clients_table.sql                 # OAuth2 클라이언트
├── V4__create_roles_tables.sql                  # 역할(RBAC)
├── V5__create_authorization_codes_table.sql     # 인가코드
├── V6__create_refresh_tokens_table.sql          # 리프레시 토큰
├── V7__create_audit_and_login_attempts_tables.sql  # 감사로그, 로그인시도
├── V8__create_signing_keys_table.sql            # JWT 서명키 (AES-256-GCM)
├── V9__create_mfa_tables.sql                    # MFA (TOTP, 복구코드)
└── V10__add_user_source_column.sql              # LDAP 사용자 소스
```

### 7.3 마이그레이션 상태 확인

```bash
# 방법 1: Flyway Maven 플러그인
mvn flyway:info \
  -Dflyway.url=jdbc:postgresql://localhost:5432/authfusion_sso \
  -Dflyway.user=authfusion \
  -Dflyway.password=authfusion-secret

# 방법 2: flyway_schema_history 테이블 직접 조회
psql -U authfusion -d authfusion_sso -c "
  SELECT version, description, type, installed_on, success
  FROM flyway_schema_history
  ORDER BY installed_rank;
"

# 방법 3: Actuator 엔드포인트 (actuator/flyway 노출 시)
curl -s http://localhost:8080/actuator/flyway | python3 -m json.tool
```

### 7.4 마이그레이션 사전 검증

프로덕션 적용 전 반드시 테스트 환경에서 마이그레이션을 검증한다.

```bash
# 테스트 DB에 프로덕션 백업 복원
pg_restore -U authfusion -d authfusion_sso_test \
  --clean --if-exists backup_production.sql

# 새 마이그레이션 적용 (dry-run 목적)
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://localhost:5432/authfusion_sso_test \
  -Dflyway.user=authfusion \
  -Dflyway.password=authfusion-secret

# 마이그레이션 결과 검증
psql -U authfusion -d authfusion_sso_test -c "\dt sso_*"
```

### 7.5 롤백 SQL 준비

Flyway는 Community Edition에서 자동 롤백(Undo)을 지원하지 않으므로,
각 마이그레이션에 대응하는 롤백 SQL을 사전에 준비한다.

```sql
-- ============================================================
-- rollback-V10.sql: LDAP 사용자 소스 컬럼 롤백
-- ============================================================
DROP INDEX IF EXISTS idx_sso_users_external_id;
DROP INDEX IF EXISTS idx_sso_users_user_source;
ALTER TABLE sso_users DROP COLUMN IF EXISTS ldap_synced_at;
ALTER TABLE sso_users DROP COLUMN IF EXISTS external_id;
ALTER TABLE sso_users DROP COLUMN IF EXISTS user_source;

-- flyway_schema_history에서 해당 마이그레이션 기록 제거
DELETE FROM flyway_schema_history WHERE version = '10';
```

```sql
-- ============================================================
-- rollback-V9.sql: MFA 테이블 롤백
-- ============================================================
DROP TABLE IF EXISTS sso_mfa_pending_sessions CASCADE;
DROP TABLE IF EXISTS sso_recovery_codes CASCADE;
DROP TABLE IF EXISTS sso_totp_secrets CASCADE;

DELETE FROM flyway_schema_history WHERE version = '9';
```

```sql
-- ============================================================
-- rollback-V8.sql: 서명 키 테이블 롤백
-- 주의: 이 롤백은 모든 암호화된 서명키를 삭제한다.
--       실행 전 반드시 키 데이터를 백업할 것.
-- ============================================================
DROP TABLE IF EXISTS sso_signing_keys CASCADE;

DELETE FROM flyway_schema_history WHERE version = '8';
```

> **경고**: 롤백 SQL의 `flyway_schema_history` DELETE 문은 해당 버전의 마이그레이션
> 기록을 제거하여 Flyway가 이후 재실행할 수 있도록 한다. 이 작업은 반드시 해당 테이블/컬럼
> 변경이 먼저 롤백된 후 실행해야 한다.

---

## 8. 롤백 절차

### 8.1 롤백 판단 기준

업그레이드 후 다음 조건 중 하나라도 해당되면 즉시 롤백을 결정한다.

| 조건 | 확인 방법 |
|------|----------|
| 헬스체크 실패 (5분 이상) | `curl http://localhost:8080/actuator/health` |
| Flyway 마이그레이션 실패 | 시작 로그에 `FlywayException` 출력 |
| OIDC Discovery 응답 오류 | `curl http://localhost:8080/.well-known/openid-configuration` |
| JWKS 엔드포인트 오류 | `curl http://localhost:8080/.well-known/jwks.json` |
| JWT 서명/검증 오류 | SSO Agent 또는 클라이언트에서 `401 Unauthorized` 다수 발생 |
| 인증 플로우 중단 | 로그인 시도 시 500 오류 반환 |
| 감사 로그 기록 실패 | `sso_audit_events` 테이블에 신규 레코드 미생성 |
| 에러율 임계값 초과 | 5xx 응답 비율 > 1% (모니터링 기준) |

> **CC 환경 주의**: 롤백 시에도 감사 로그의 연속성을 보장해야 한다 (FAU_GEN.1).
> 롤백 작업 자체도 감사 로그에 SYSTEM 이벤트로 기록되어야 한다.

### 8.2 JAR 배포 롤백

```bash
# ── 단계 1: 서비스 즉시 중지 ──
systemctl stop authfusion-sso

# ── 단계 2: 이전 JAR 복원 ──
cp /opt/authfusion/sso-server.jar.bak.$(date +%Y%m%d) \
   /opt/authfusion/sso-server.jar

# ── 단계 3: 설정 파일 복원 (변경된 경우) ──
cp -r /opt/authfusion/config.bak.$(date +%Y%m%d)/* \
   /opt/authfusion/config/

# ── 단계 4: DB 롤백 (마이그레이션이 실행된 경우) ──
# V10 → V9 롤백 예시
psql -U authfusion -d authfusion_sso -f rollback-V10.sql

# 또는 전체 DB 복원 (마이그레이션이 복잡한 경우)
pg_restore -U authfusion -d authfusion_sso \
  --clean --if-exists \
  /opt/authfusion/backup/db_20260304_120000.sql

# ── 단계 5: 서비스 시작 ──
systemctl start authfusion-sso

# ── 단계 6: 롤백 후 검증 ──
curl -sf http://localhost:8080/actuator/health && echo "정상"
curl -sf http://localhost:8080/.well-known/openid-configuration | head -5
```

### 8.3 Docker Compose 롤백

```bash
# ── 단계 1: SSO Server 중지 ──
docker compose stop sso-server

# ── 단계 2: 이전 이미지로 변경 ──
# 방법 A: 이전 태그로 직접 변경
# docker-compose.yml에서 이미지 태그를 이전 버전으로 수정
# 또는 이전에 tag 해둔 이미지 사용
docker tag authfusion/sso-server:1.0.0 authfusion/sso-server:latest

# 방법 B: 에어갭 환경 - 이전 이미지 로드
docker load -i authfusion-sso-server-1.0.0.tar

# ── 단계 3: DB 롤백 (필요 시) ──
# 선택적 SQL 롤백
docker exec -i authfusion-postgres \
  psql -U authfusion -d authfusion_sso < rollback-V10.sql

# 또는 전체 DB 복원
docker exec -i authfusion-postgres \
  pg_restore -U authfusion -d authfusion_sso \
  --clean --if-exists < backup_20260304.sql

# ── 단계 4: 서비스 재시작 ──
docker compose up -d --no-deps sso-server

# ── 단계 5: CC 모드 롤백 시 ──
docker compose \
  -f docker-compose.yml \
  -f docker-compose.cc.yml \
  up -d --no-deps sso-server

# ── 단계 6: 롤백 후 검증 ──
docker compose exec sso-server \
  wget -qO- http://localhost:8080/actuator/health
docker compose logs --tail=50 sso-server
```

### 8.4 SSO Agent 롤백

```bash
# 레거시 앱 중지
systemctl stop legacy-app

# Agent JAR 복원
cp /opt/legacy-app/lib/sso-agent-1.0.0.jar.bak \
   /opt/legacy-app/lib/sso-agent.jar

# 레거시 앱 시작
systemctl start legacy-app

# JWKS 통신 확인
curl -s http://legacy-app:8081/health
```

---

## 9. 업그레이드 후 검증 절차

### 9.1 기본 검증

```bash
# 1. 헬스체크
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# 2. 버전 확인 (info 엔드포인트)
curl -s http://localhost:8080/actuator/info | python3 -m json.tool

# 3. OIDC Discovery 검증
curl -s http://localhost:8080/.well-known/openid-configuration | python3 -m json.tool

# 4. JWKS 검증 (키가 정상적으로 로드되는지)
curl -s http://localhost:8080/.well-known/jwks.json | python3 -m json.tool
```

### 9.2 인증 플로우 검증

```bash
# 1. 로그인 테스트
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin-password"}' \
  -w "\nHTTP Status: %{http_code}\n"

# 2. 토큰 발급 테스트 (Client Credentials)
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=test-client&client_secret=test-secret" \
  -w "\nHTTP Status: %{http_code}\n"

# 3. 사용자 정보 조회 (Bearer Token)
TOKEN=$(curl -s -X POST http://localhost:8080/oauth2/token \
  -d "grant_type=client_credentials&client_id=test-client&client_secret=test-secret" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
curl -s http://localhost:8080/oauth2/userinfo \
  -H "Authorization: Bearer ${TOKEN}" \
  -w "\nHTTP Status: %{http_code}\n"
```

### 9.3 CC 모드 검증 (CC 환경만)

```bash
# 1. 확장 기능 비활성화 확인
# 아래 URL은 모두 403 또는 404를 반환해야 한다
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html
# 기대값: 404

# 2. 확장 컨트롤러 비활성화 확인
# 확장 API 호출 시 403/404 응답
curl -s -o /dev/null -w "%{http_code}" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  http://localhost:8080/api/v1/clients
# CC 모드에서 기대값: 403

# 3. Actuator 최소 노출 확인
curl -s http://localhost:8080/actuator | python3 -m json.tool
# health만 노출되어야 함

# 4. 감사 로그 기록 확인
psql -U authfusion -d authfusion_sso -c "
  SELECT event_type, action, result, created_at
  FROM sso_audit_events
  ORDER BY created_at DESC
  LIMIT 10;
"
```

### 9.4 데이터 무결성 검증

```bash
# 주요 테이블 레코드 수 비교 (업그레이드 전후)
psql -U authfusion -d authfusion_sso -c "
  SELECT 'sso_users' AS table_name, COUNT(*) FROM sso_users
  UNION ALL
  SELECT 'sso_clients', COUNT(*) FROM sso_clients
  UNION ALL
  SELECT 'sso_roles', COUNT(*) FROM sso_roles
  UNION ALL
  SELECT 'sso_signing_keys', COUNT(*) FROM sso_signing_keys
  UNION ALL
  SELECT 'sso_audit_events', COUNT(*) FROM sso_audit_events
  UNION ALL
  SELECT 'sso_totp_secrets', COUNT(*) FROM sso_totp_secrets;
"
```

---

## 10. 무중단 업그레이드 전략

### 10.1 블루-그린 배포

운영환경에서 서비스 중단을 최소화하기 위한 블루-그린 배포 전략이다.

```
┌──────────────────────────────────────────────────────────┐
│                    Nginx / Load Balancer                  │
│                    (리버스 프록시)                         │
└──────────────────────┬───────────────────────────────────┘
                       │
          ┌────────────┴────────────┐
          │                         │
   ┌──────▼──────┐          ┌──────▼──────┐
   │  Blue (현재) │          │ Green (신규) │
   │ SSO v1.0.0  │          │ SSO v1.1.0  │
   │ :8080       │          │ :8081       │
   └──────┬──────┘          └──────┬──────┘
          │                         │
          └────────────┬────────────┘
                       │
                ┌──────▼──────┐
                │ PostgreSQL  │
                │ (공유 DB)   │
                └─────────────┘
```

#### 절차

```bash
# 1. Green 인스턴스 기동 (신규 버전, 다른 포트)
java -jar sso-server-1.1.0.jar \
  --server.port=8081 \
  --spring.profiles.active=cc \
  &

# 2. Green 헬스체크
curl -sf http://localhost:8081/actuator/health

# 3. Nginx upstream 전환
# /etc/nginx/conf.d/authfusion.conf 에서 upstream 변경
#   server 127.0.0.1:8080 → server 127.0.0.1:8081
nginx -s reload

# 4. Blue 인스턴스 트래픽 드레이닝 (기존 세션 종료 대기)
sleep 60

# 5. Blue 인스턴스 중지
kill $(cat /opt/authfusion/blue.pid)

# 6. 검증 후 Blue를 신규 버전으로 교체 (다음 롤백 대비)
```

### 10.2 제약 사항

무중단 업그레이드 시 다음 제약 사항에 유의한다.

| 항목 | 제약 | 대응 방안 |
|------|------|----------|
| DB 마이그레이션 | 두 버전이 동시 접속할 수 있어야 함 | 하위 호환 마이그레이션만 허용 |
| JWT 서명키 | Blue/Green이 동일 키를 사용해야 함 | DB 기반 키 관리로 자동 공유 |
| 세션 | 세션 데이터 호환성 필요 | DB 세션이므로 자동 공유 |
| Flyway lock | 동시 실행 시 lock 충돌 가능 | Green 기동 시 Blue 중지 후 실행 |
| 스키마 호환 | 컬럼 삭제/이름 변경 불가 | 컬럼 추가만 허용, 삭제는 다음 버전 |

> **참고**: AuthFusion SSO Server는 JWT 서명키와 세션을 PostgreSQL에 저장하므로
> 블루-그린 배포 시 두 인스턴스가 동일한 키와 세션을 공유한다. 단, Flyway 마이그레이션은
> 한 인스턴스에서만 실행되어야 한다.

---

## 11. 특수 시나리오

### 11.1 마스터 키 변경 시 키 재암호화

`AUTHFUSION_KEY_MASTER_SECRET` 환경변수를 변경해야 하는 경우, 기존 암호화된 서명키를
새 마스터 키로 재암호화해야 한다. 키 암호화는 `KeyEncryptionService`가 담당한다.

```bash
# 1. 서비스 중지
systemctl stop authfusion-sso

# 2. 기존 서명키 백업
pg_dump -U authfusion -t sso_signing_keys authfusion_sso > signing_keys_backup.sql

# 3. 키 재암호화 유틸리티 실행 (향후 제공 예정)
#    현재는 수동 절차:
#    a) 기존 마스터 키로 복호화
#    b) 새 마스터 키로 재암호화
#    c) DB 업데이트

# 4. 새 마스터 키 환경변수 설정
export AUTHFUSION_KEY_MASTER_SECRET=<새-마스터-시크릿>

# 5. 서비스 시작
systemctl start authfusion-sso

# 6. JWKS 응답 검증 (기존 키가 정상 로드되는지)
curl -s http://localhost:8080/.well-known/jwks.json | python3 -m json.tool
```

### 11.2 Major 버전 업그레이드 (v1.x → v2.x)

Major 버전 업그레이드는 하위 호환성이 깨지므로 특별한 주의가 필요하다.

```
권장 절차:
1. 신규 버전 릴리즈 노트의 "Breaking Changes" 섹션 정독
2. 전용 테스트 환경 구축 → 프로덕션 백업 복원
3. 마이그레이션 가이드에 따라 단계적 업그레이드 실행
4. 모든 연동 클라이언트(SSO Agent 포함) 호환성 테스트
5. 충분한 소크 테스트 기간 (최소 1주일 권장)
6. 프로덕션 적용 시 반드시 유지보수 윈도우 확보
```

### 11.3 Flyway 마이그레이션 실패 시 복구

```bash
# 1. 실패한 마이그레이션 확인
psql -U authfusion -d authfusion_sso -c "
  SELECT version, description, success, installed_on
  FROM flyway_schema_history
  WHERE success = FALSE;
"

# 2. 실패한 마이그레이션 수동 정리
#    (Flyway repair는 실패한 마이그레이션 레코드를 제거)
mvn flyway:repair \
  -Dflyway.url=jdbc:postgresql://localhost:5432/authfusion_sso \
  -Dflyway.user=authfusion \
  -Dflyway.password=authfusion-secret

# 3. 원인 분석 및 수정 후 재실행
#    또는 전체 DB 복원 후 재시도
pg_restore -U authfusion -d authfusion_sso \
  --clean --if-exists backup_before_upgrade.sql
```

### 11.4 TOTP MFA 데이터 마이그레이션

TOTP 비밀키는 AES-256-GCM으로 암호화되어 `sso_totp_secrets` 테이블에 저장된다.
마스터 키가 동일하면 업그레이드 시 별도 데이터 마이그레이션이 필요하지 않다.

```bash
# TOTP 데이터 무결성 확인
psql -U authfusion -d authfusion_sso -c "
  SELECT
    COUNT(*) as total,
    COUNT(CASE WHEN verified THEN 1 END) as verified,
    COUNT(CASE WHEN enabled THEN 1 END) as enabled
  FROM sso_totp_secrets;
"
```

---

## 12. LTS 브랜치 및 백포트 정책

### 12.1 LTS 브랜치 구조

```
main (개발)
 ├── release/sso-1.x-lts    # v1.x LTS 브랜치
 │    ├── sso-v1.0.0
 │    ├── sso-v1.0.1         # 보안 패치
 │    └── sso-v1.0.2         # 버그 수정
 ├── release/sso-2.x-lts    # v2.x LTS 브랜치 (향후)
 └── feature/*               # 기능 개발 브랜치
```

### 12.2 LTS 정책

| 항목 | 정책 |
|------|------|
| LTS 지원 기간 | 최소 3년 (CC 인증 유효기간 연계) |
| 허용 변경 | 보안 패치, 버그 수정만 |
| 금지 변경 | 새 기능 추가, API 변경, DB 스키마 변경 (추가만 제한적 허용) |
| 코드 리뷰 | 최소 2인 리뷰 필수 |
| 테스트 | 전체 회귀 테스트 통과 필수 |
| CC 인증 | 패치 적용 시 인증 영향 범위 평가 |

### 12.3 백포트 절차

```bash
# 1. main에서 수정 커밋 확인
git log --oneline main -- "path/to/fixed/file"

# 2. LTS 브랜치로 전환
git checkout release/sso-1.x-lts

# 3. 체리픽
git cherry-pick <commit-hash>

# 4. 충돌 해결 후 커밋
git add .
git commit

# 5. LTS 브랜치 테스트 실행
mvn clean verify

# 6. 패치 버전 태깅
git tag sso-v1.0.2

# 7. CC 빌드 (SBOM + 서명)
mvn clean package -Pcc
```

---

## 13. 운영 팁

### 13.1 자동화 스크립트 예시

```bash
#!/bin/bash
# upgrade-sso.sh - AuthFusion SSO Server 업그레이드 자동화
set -euo pipefail

VERSION=${1:?"Usage: $0 <version> (e.g., 1.1.0)"}
SSO_HOME=/opt/authfusion
BACKUP_DIR=${SSO_HOME}/backup/$(date +%Y%m%d_%H%M%S)

echo "=== AuthFusion SSO Server 업그레이드 시작: v${VERSION} ==="

# 1. 사전 검증
echo "[1/7] 아티팩트 검증..."
sha256sum -c sso-server-${VERSION}.sha256

# 2. 백업 디렉터리 생성
mkdir -p ${BACKUP_DIR}

# 3. DB 백업
echo "[2/7] 데이터베이스 백업..."
pg_dump -U authfusion --format=custom authfusion_sso \
  > ${BACKUP_DIR}/db_backup.sql

# 4. 서비스 중지
echo "[3/7] 서비스 중지..."
systemctl stop authfusion-sso

# 5. JAR 및 설정 백업
echo "[4/7] 파일 백업..."
cp ${SSO_HOME}/sso-server.jar ${BACKUP_DIR}/
cp -r ${SSO_HOME}/config ${BACKUP_DIR}/

# 6. JAR 교체
echo "[5/7] JAR 교체..."
cp sso-server-${VERSION}.jar ${SSO_HOME}/sso-server.jar

# 7. 서비스 시작
echo "[6/7] 서비스 시작..."
systemctl start authfusion-sso

# 8. 헬스체크
echo "[7/7] 헬스체크 (최대 90초 대기)..."
for i in $(seq 1 18); do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "업그레이드 성공! v${VERSION}"
    exit 0
  fi
  sleep 5
done

echo "ERROR: 헬스체크 실패. 롤백을 고려하세요."
echo "롤백 명령: ./rollback-sso.sh ${BACKUP_DIR}"
exit 1
```

### 13.2 업그레이드 이력 관리

```
/opt/authfusion/
├── sso-server.jar              # 현재 운영 JAR
├── config/                     # 현재 설정
├── logs/                       # 로그
├── releases/                   # 릴리즈 아티팩트 보관
│   ├── v1.0.0/
│   ├── v1.0.1/
│   └── v1.1.0/
└── backup/                     # 업그레이드별 백업
    ├── 20260301_120000/        # 타임스탬프별 백업
    │   ├── db_backup.sql
    │   ├── sso-server.jar
    │   └── config/
    └── 20260304_150000/
```

### 13.3 모니터링 포인트

업그레이드 직후 집중 모니터링해야 할 메트릭:

| 메트릭 | 정상 범위 | 확인 방법 |
|--------|----------|----------|
| HTTP 5xx 비율 | < 0.1% | 로드밸런서/Nginx 로그 |
| 로그인 성공률 | > 99% | 감사 로그 AUTHENTICATION 이벤트 |
| JWT 발급 수 | 기존 대비 +-10% | 감사 로그 TOKEN_OPERATION 이벤트 |
| 응답 시간 (P95) | < 500ms | Actuator metrics |
| DB 커넥션 풀 | 사용률 < 80% | HikariCP 메트릭 |
| JVM 힙 사용량 | < 80% | Actuator JVM 메트릭 |

---

## 14. 문제 해결 (Troubleshooting)

### 14.1 자주 발생하는 문제

| 증상 | 원인 | 해결 |
|------|------|------|
| 시작 시 `FlywayException` | 마이그레이션 SQL 오류 | 7.5절 Flyway 복구 절차 참조 |
| `MasterSecretNotFoundException` | 마스터 시크릿 미설정 | `AUTHFUSION_KEY_MASTER_SECRET` 환경변수 확인 |
| `DecryptionException` | 마스터 키 불일치 | 이전 마스터 키 복원 또는 11.1절 키 재암호화 |
| JWKS 응답에 키가 없음 | 서명키 로드 실패 | `sso_signing_keys` 테이블 확인, 키 재생성 |
| Agent에서 401 지속 | Server-Agent 버전 불일치 | 2.2절 호환성 매트릭스 확인 |
| DB 커넥션 풀 고갈 | HikariCP 설정 부족 | `maximum-pool-size` 조정 |
| OOM (OutOfMemory) | JVM 힙 부족 | `-Xmx` 값 증가 (기본 권장: 512MB) |

### 14.2 로그 분석

```bash
# Flyway 마이그레이션 로그 확인
grep -i "flyway\|migration" /opt/authfusion/logs/sso-server.log

# JWT 서명키 관련 로그
grep -i "KeyPairManager\|KeyEncryption\|signing.key" \
  /opt/authfusion/logs/sso-server.log

# 인증 오류 로그
grep -i "authentication\|unauthorized\|forbidden" \
  /opt/authfusion/logs/sso-server.log

# Docker 환경 로그
docker compose logs sso-server 2>&1 | grep -i "error\|warn\|flyway"
```

---

## 15. 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| 1.0 | 2026-03-04 | 최초 작성 |
