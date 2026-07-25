#!/usr/bin/env bash
# 새 도메인의 nested 멀티모듈 디렉터리 구조 생성
# 사용법: ./.claude/scripts/new-module.sh <도메인명>
# 예시:   ./.claude/scripts/new-module.sh company

set -euo pipefail

DOMAIN=${1:-}
if [[ -z "$DOMAIN" ]]; then
    echo "Usage: $0 <domain-name>"
    echo "Example: $0 company"
    exit 1
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASE="com/yapp/todakun"
# nested 도메인은 외부 래퍼 디렉터리에만 `module-` 접두사를 붙인다 (내부 레이어 모듈은 평이한 이름 유지)
DOMAIN_DIR="$ROOT/module-$DOMAIN"

echo "Creating module structure for: $DOMAIN (dir: module-$DOMAIN)"

mkdir -p "$DOMAIN_DIR/$DOMAIN-domain/src/main/kotlin/$BASE/$DOMAIN"
mkdir -p "$DOMAIN_DIR/$DOMAIN-domain/src/test/kotlin/$BASE/$DOMAIN"
mkdir -p "$DOMAIN_DIR/$DOMAIN-application/src/main/kotlin/$BASE/$DOMAIN/application"
mkdir -p "$DOMAIN_DIR/$DOMAIN-application/src/test/kotlin/$BASE/$DOMAIN/application"
mkdir -p "$DOMAIN_DIR/$DOMAIN-adapter-in/src/main/kotlin/$BASE/$DOMAIN/adapter/web"
mkdir -p "$DOMAIN_DIR/$DOMAIN-adapter-in/src/test/kotlin/$BASE/$DOMAIN/adapter/web"
mkdir -p "$DOMAIN_DIR/$DOMAIN-adapter-out/src/main/kotlin/$BASE/$DOMAIN/adapter/persistence"
mkdir -p "$DOMAIN_DIR/$DOMAIN-adapter-out/src/main/java/$BASE/$DOMAIN/adapter/persistence"
mkdir -p "$DOMAIN_DIR/$DOMAIN-adapter-out/src/test/kotlin/$BASE/$DOMAIN/adapter/persistence"

echo "✓ Directories created for '$DOMAIN'"
echo ""
echo "Next steps:"
echo "  1. settings.gradle.kts 에 4개 모듈 추가"
echo "  2. module-$DOMAIN/*/build.gradle.kts 생성"
echo "  3. bootstrap/build.gradle.kts 에 의존성 추가"
echo "  4. architecture-test/build.gradle.kts 에 testImplementation 추가"
echo "  5. /new-domain $DOMAIN 실행하여 소스 파일 스캐폴딩"
