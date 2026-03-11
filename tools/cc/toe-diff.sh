#!/usr/bin/env bash
# ============================================================================
# TOE Diff Tool
# TOE 범위 내 변경 파일을 감지하고 보고하는 도구
# ============================================================================
set -euo pipefail

BASE_BRANCH="${1:-origin/main}"
TOE_PATHS=(
  "products/sso-server/src/"
  "products/sso-agent/src/"
)

echo "=== AuthFusion TOE Diff Tool ==="
echo "Base: $BASE_BRANCH"
echo "HEAD: $(git rev-parse --short HEAD)"
echo ""

TOTAL_CHANGES=0

for path in "${TOE_PATHS[@]}"; do
  CHANGES=$(git diff --name-only "$BASE_BRANCH"...HEAD -- "$path" 2>/dev/null | wc -l)
  TOTAL_CHANGES=$((TOTAL_CHANGES + CHANGES))

  if [ "$CHANGES" -gt 0 ]; then
    echo "--- $path ($CHANGES files) ---"
    git diff --name-only "$BASE_BRANCH"...HEAD -- "$path" | sort
    echo ""
  fi
done

echo "=== Summary ==="
echo "Total TOE changes: $TOTAL_CHANGES files"

if [ "$TOTAL_CHANGES" -gt 0 ]; then
  echo ""
  echo "[WARNING] TOE 범위 내 변경이 감지되었습니다."
  echo "CC 영향 분석이 필요합니다."
  exit 1
else
  echo "TOE 범위 내 변경 없음."
  exit 0
fi
