#!/usr/bin/env bash
# Stop hook: 세션 종료 시 ktlint 자동 포맷 (실패해도 세션 종료를 막지 않음)

set -euo pipefail

PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-.}"
cd "$PROJECT_ROOT" || exit 0
[[ -f "./gradlew" ]] || exit 0

if ! OUTPUT=$(./gradlew ktlintFormat --quiet 2>&1 | tail -5); then
    echo "⚠️ ktlintFormat 실패:" >&2
    echo "$OUTPUT" >&2
fi

exit 0