#!/usr/bin/env bash
# ============================================================================
# AuthFusion Platform - 릴리즈 아티팩트 서명 스크립트
# GPG 서명 및 SHA-256 체크섬을 생성합니다.
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
ARTIFACTS_DIR="$ROOT_DIR/release/artifacts"
CHECKSUMS_DIR="$ARTIFACTS_DIR/checksums"

echo "=== AuthFusion Artifact Signing ==="

mkdir -p "$CHECKSUMS_DIR"

# GPG key check
GPG_KEY="${AUTHFUSION_GPG_KEY_ID:-}"
if [ -z "$GPG_KEY" ]; then
  echo "WARN: AUTHFUSION_GPG_KEY_ID not set. Skipping GPG signing."
  echo "      Set the environment variable to enable GPG signing."
  SKIP_GPG=true
else
  SKIP_GPG=false
  echo "Using GPG Key: $GPG_KEY"
fi

# Find artifacts to sign
ARTIFACTS=(
  "$ROOT_DIR/products/sso-server/target/sso-server-*.jar"
  "$ROOT_DIR/products/sso-agent/target/sso-agent-*.jar"
  "$ARTIFACTS_DIR"/*-bom.json
)

for pattern in "${ARTIFACTS[@]}"; do
  for artifact in $pattern; do
    if [ ! -f "$artifact" ]; then
      continue
    fi

    filename=$(basename "$artifact")
    echo ""
    echo "--- Processing: $filename ---"

    # SHA-256 checksum
    sha256sum "$artifact" | awk '{print $1}' > "$CHECKSUMS_DIR/${filename}.sha256"
    echo "  [OK] SHA-256: $(cat "$CHECKSUMS_DIR/${filename}.sha256")"

    # GPG signature
    if [ "$SKIP_GPG" = false ]; then
      gpg --batch --yes --armor --detach-sign \
        --default-key "$GPG_KEY" \
        --output "$CHECKSUMS_DIR/${filename}.asc" \
        "$artifact"
      echo "  [OK] GPG signature created"
    fi
  done
done

echo ""
echo "=== Signing Complete ==="
echo "Checksums:"
ls -la "$CHECKSUMS_DIR"/ 2>/dev/null
