#!/usr/bin/env bash
# ============================================================================
# CC Config Linter
# CC 모드 설정 검증 도구
# ============================================================================
set -euo pipefail

ERRORS=0
WARNINGS=0

echo "=== AuthFusion CC Config Linter ==="
echo ""

# Check application-cc.yml exists
CC_CONFIG="products/sso-server/src/main/resources/application-cc.yml"
if [ ! -f "$CC_CONFIG" ]; then
  echo "[ERROR] CC 설정 파일이 없습니다: $CC_CONFIG"
  ERRORS=$((ERRORS + 1))
else
  echo "[OK] CC 설정 파일 존재: $CC_CONFIG"

  # Check extended-features-enabled is false
  if grep -q "extended-features-enabled: false" "$CC_CONFIG"; then
    echo "[OK] 확장 기능 비활성화 설정 확인"
  else
    echo "[ERROR] CC 모드에서 extended-features-enabled: false 필수"
    ERRORS=$((ERRORS + 1))
  fi
fi

# Check @ToeScope annotations exist
TOE_COUNT=$(grep -rl "@ToeScope" products/sso-server/src/ 2>/dev/null | wc -l)
echo ""
echo "[INFO] @ToeScope 어노테이션 파일 수: $TOE_COUNT"
if [ "$TOE_COUNT" -lt 10 ]; then
  echo "[WARNING] @ToeScope 어노테이션이 적습니다 (최소 10개 이상 권장)"
  WARNINGS=$((WARNINGS + 1))
fi

# Check @ExtendedFeature annotations
EXT_COUNT=$(grep -rl "@ExtendedFeature" products/sso-server/src/ 2>/dev/null | wc -l)
echo "[INFO] @ExtendedFeature 어노테이션 파일 수: $EXT_COUNT"

# Check no @ExtendedFeature in TOE-only code
CONFLICT=$(grep -rl "@ToeScope" products/sso-server/src/ 2>/dev/null | xargs grep -l "@ExtendedFeature" 2>/dev/null | wc -l)
if [ "$CONFLICT" -gt 0 ]; then
  echo "[ERROR] @ToeScope와 @ExtendedFeature가 동시 적용된 파일: $CONFLICT 개"
  grep -rl "@ToeScope" products/sso-server/src/ 2>/dev/null | xargs grep -l "@ExtendedFeature" 2>/dev/null
  ERRORS=$((ERRORS + 1))
fi

# Check Maven CC profile
POM="products/sso-server/pom.xml"
if grep -q "<id>cc</id>" "$POM" 2>/dev/null; then
  echo "[OK] Maven CC 프로파일 존재"
else
  echo "[WARNING] Maven CC 프로파일이 없습니다"
  WARNINGS=$((WARNINGS + 1))
fi

echo ""
echo "=== Summary ==="
echo "Errors: $ERRORS"
echo "Warnings: $WARNINGS"

if [ "$ERRORS" -gt 0 ]; then
  echo "[FAIL] CC 설정 검증 실패"
  exit 1
else
  echo "[PASS] CC 설정 검증 통과"
  exit 0
fi
