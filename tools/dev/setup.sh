#!/usr/bin/env bash
# ============================================================================
# AuthFusion Platform - 개발 환경 설정 스크립트
# ============================================================================
set -euo pipefail

echo "=== AuthFusion Platform 개발 환경 설정 ==="
echo ""

# Check Java
if command -v java &> /dev/null; then
  JAVA_VER=$(java -version 2>&1 | head -1)
  echo "[OK] Java: $JAVA_VER"
else
  echo "[ERROR] Java 17 이상이 필요합니다."
  echo "  설치: https://adoptium.net/temurin/releases/"
  exit 1
fi

# Check Maven
if command -v mvn &> /dev/null; then
  MVN_VER=$(mvn --version 2>&1 | head -1)
  echo "[OK] Maven: $MVN_VER"
else
  echo "[WARNING] Maven이 설치되어 있지 않습니다."
  echo "  Maven wrapper (./mvnw)를 사용하거나 설치하세요."
fi

# Check Node.js
if command -v node &> /dev/null; then
  NODE_VER=$(node --version)
  echo "[OK] Node.js: $NODE_VER"
else
  echo "[ERROR] Node.js 20 이상이 필요합니다."
  echo "  설치: https://nodejs.org/"
  exit 1
fi

# Check Docker
if command -v docker &> /dev/null; then
  DOCKER_VER=$(docker --version)
  echo "[OK] Docker: $DOCKER_VER"
else
  echo "[WARNING] Docker가 설치되어 있지 않습니다."
  echo "  Full stack 실행을 위해 Docker가 필요합니다."
fi

echo ""
echo "=== 의존성 설치 ==="

# SSO Server
echo "SSO Server 빌드..."
if [ -f "products/sso-server/pom.xml" ]; then
  cd products/sso-server && mvn dependency:resolve -q 2>/dev/null && cd ../..
  echo "[OK] SSO Server 의존성 설치 완료"
fi

# SSO Agent
echo "SSO Agent 빌드..."
if [ -f "products/sso-agent/pom.xml" ]; then
  cd products/sso-agent && mvn dependency:resolve -q 2>/dev/null && cd ../..
  echo "[OK] SSO Agent 의존성 설치 완료"
fi

# Admin Console
echo "Admin Console 의존성 설치..."
if [ -f "products/admin-console/package.json" ]; then
  cd products/admin-console && npm install && cd ../..
  echo "[OK] Admin Console 의존성 설치 완료"
fi

echo ""
echo "=== 환경 변수 ==="
echo "다음 환경 변수를 설정하세요:"
echo "  export AUTHFUSION_KEY_MASTER_SECRET=<마스터 시크릿>"
echo "  export AUTHFUSION_LDAP_BIND_PASSWORD=<LDAP 비밀번호> (선택)"
echo ""
echo "=== 실행 ==="
echo "  Full stack: docker compose up -d"
echo "  SSO Server: cd products/sso-server && mvn spring-boot:run"
echo "  Admin Console: cd products/admin-console && npm run dev"
echo ""
echo "개발 환경 설정 완료!"
