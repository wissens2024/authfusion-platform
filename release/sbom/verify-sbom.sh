#!/usr/bin/env bash
# ============================================================================
# AuthFusion Platform - SBOM 검증 스크립트
# 생성된 SBOM의 유효성을 검증합니다.
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
ARTIFACTS_DIR="$ROOT_DIR/release/artifacts"

echo "=== AuthFusion SBOM Verification ==="

ERRORS=0

for bom_file in "$ARTIFACTS_DIR"/*-bom.json; do
  if [ ! -f "$bom_file" ]; then
    echo "WARN: No SBOM files found in $ARTIFACTS_DIR"
    exit 1
  fi

  filename=$(basename "$bom_file")
  echo ""
  echo "--- Verifying: $filename ---"

  # Check JSON validity
  if python3 -m json.tool "$bom_file" > /dev/null 2>&1; then
    echo "  [OK] Valid JSON"
  else
    echo "  [FAIL] Invalid JSON"
    ERRORS=$((ERRORS + 1))
    continue
  fi

  # Check CycloneDX format
  if grep -q '"bomFormat"' "$bom_file" 2>/dev/null; then
    echo "  [OK] CycloneDX format detected"
  else
    echo "  [WARN] bomFormat field not found"
  fi

  # Count components
  COMP_COUNT=$(python3 -c "import json; f=open('$bom_file'); d=json.load(f); print(len(d.get('components',[])))" 2>/dev/null || echo "?")
  echo "  [INFO] Components: $COMP_COUNT"

  # Check for known vulnerable patterns (basic)
  if grep -qi '"log4j-core"' "$bom_file" 2>/dev/null; then
    echo "  [WARN] log4j-core detected - verify version"
  fi
done

echo ""
if [ $ERRORS -eq 0 ]; then
  echo "=== All SBOM files verified successfully ==="
else
  echo "=== SBOM verification failed ($ERRORS errors) ==="
  exit 1
fi
