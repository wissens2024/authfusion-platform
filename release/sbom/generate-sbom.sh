#!/usr/bin/env bash
# ============================================================================
# AuthFusion Platform - SBOM (Software Bill of Materials) 생성 스크립트
# CycloneDX 형식으로 TOE 구성요소의 SBOM을 생성합니다.
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/release/artifacts"

echo "=== AuthFusion SBOM Generation ==="
echo "Root: $ROOT_DIR"
echo "Output: $OUTPUT_DIR"

mkdir -p "$OUTPUT_DIR"

# SSO Server SBOM (Maven CycloneDX Plugin)
echo ""
echo "--- SSO Server SBOM ---"
cd "$ROOT_DIR/products/sso-server"
mvn org.cyclonedx:cyclonedx-maven-plugin:2.7.11:makeAggregateBom \
  -DoutputFormat=json \
  -DoutputName=sso-server-bom \
  -DincludeCompileScope=true \
  -DincludeRuntimeScope=true \
  -q

cp target/sso-server-bom.json "$OUTPUT_DIR/sso-server-bom.json"
echo "Generated: $OUTPUT_DIR/sso-server-bom.json"

# SSO Agent SBOM
echo ""
echo "--- SSO Agent SBOM ---"
cd "$ROOT_DIR/products/sso-agent"
mvn org.cyclonedx:cyclonedx-maven-plugin:2.7.11:makeAggregateBom \
  -DoutputFormat=json \
  -DoutputName=sso-agent-bom \
  -DincludeCompileScope=true \
  -DincludeRuntimeScope=true \
  -q

cp target/sso-agent-bom.json "$OUTPUT_DIR/sso-agent-bom.json"
echo "Generated: $OUTPUT_DIR/sso-agent-bom.json"

# Admin Console SBOM (npm-based)
echo ""
echo "--- Admin Console SBOM ---"
cd "$ROOT_DIR/products/admin-console"
if command -v npx &> /dev/null; then
  npx @cyclonedx/cyclonedx-npm --output-file "$OUTPUT_DIR/admin-console-bom.json" --output-format JSON 2>/dev/null || \
    echo "WARN: CycloneDX npm plugin not available. Install with: npm i -g @cyclonedx/cyclonedx-npm"
fi

echo ""
echo "=== SBOM Generation Complete ==="
ls -la "$OUTPUT_DIR"/*-bom.json 2>/dev/null || echo "No SBOM files generated"
