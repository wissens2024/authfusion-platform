#!/usr/bin/env bash
# ============================================================================
# AuthFusion Platform - 아티팩트 서명 검증 스크립트
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
CHECKSUMS_DIR="$ROOT_DIR/release/artifacts/checksums"

echo "=== AuthFusion Signature Verification ==="

ERRORS=0

for sha_file in "$CHECKSUMS_DIR"/*.sha256; do
  if [ ! -f "$sha_file" ]; then
    echo "No checksum files found"
    exit 1
  fi

  artifact_name=$(basename "$sha_file" .sha256)
  echo ""
  echo "--- Verifying: $artifact_name ---"

  # Find the original artifact
  ARTIFACT=$(find "$ROOT_DIR" -name "$artifact_name" -not -path "*/checksums/*" 2>/dev/null | head -1)
  if [ -z "$ARTIFACT" ]; then
    echo "  [SKIP] Artifact not found"
    continue
  fi

  # Verify SHA-256
  EXPECTED=$(cat "$sha_file")
  ACTUAL=$(sha256sum "$ARTIFACT" | awk '{print $1}')
  if [ "$EXPECTED" = "$ACTUAL" ]; then
    echo "  [OK] SHA-256 checksum matches"
  else
    echo "  [FAIL] SHA-256 mismatch!"
    echo "    Expected: $EXPECTED"
    echo "    Actual:   $ACTUAL"
    ERRORS=$((ERRORS + 1))
  fi

  # Verify GPG signature
  ASC_FILE="$CHECKSUMS_DIR/${artifact_name}.asc"
  if [ -f "$ASC_FILE" ]; then
    if gpg --verify "$ASC_FILE" "$ARTIFACT" 2>/dev/null; then
      echo "  [OK] GPG signature valid"
    else
      echo "  [FAIL] GPG signature invalid"
      ERRORS=$((ERRORS + 1))
    fi
  else
    echo "  [INFO] No GPG signature found"
  fi
done

echo ""
if [ $ERRORS -eq 0 ]; then
  echo "=== All verifications passed ==="
else
  echo "=== Verification failed ($ERRORS errors) ==="
  exit 1
fi
