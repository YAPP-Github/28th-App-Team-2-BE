#!/usr/bin/env bash
# Stop hook: 세션 종료 시 ktlint 자동 포맷 + 추적 파일 EOF 개행 자동 보정 (실패해도 세션 종료를 막지 않음)

set -euo pipefail

PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-.}"
cd "$PROJECT_ROOT" || exit 0
[[ -f "./gradlew" ]] || exit 0

if ! OUTPUT=$(./gradlew ktlintFormat --quiet 2>&1 | tail -5); then
    echo "⚠️ ktlintFormat 실패:" >&2
    echo "$OUTPUT" >&2
fi

# ConventionTest가 검증하는 "개행 문자로 끝난다" 규칙을 동일한 대상(binary 확장자 제외)에 대해 자동 보정한다.
BINARY_EXTENSIONS="jar png jpg jpeg gif ico svg woff woff2 ttf class keystore p12"

while IFS= read -r file; do
    [[ -f "$file" ]] || continue
    [[ -s "$file" ]] || continue

    ext_lower=$(echo "${file##*.}" | tr '[:upper:]' '[:lower:]')
    for bin_ext in $BINARY_EXTENSIONS; do
        [[ "$ext_lower" == "$bin_ext" ]] && continue 2
    done

    last_byte=$(tail -c 1 "$file" | od -An -tx1 | tr -d ' ')
    if [[ "$last_byte" != "0a" ]]; then
        printf '\n' >>"$file"
        echo "개행 문자 추가: $file"
    fi
done < <(git ls-files)

exit 0
