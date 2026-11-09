#!/usr/bin/env bash
# PostToolUse hook: .kt 파일 수정 시 ktlint 자동 포맷
# Claude Code가 Edit/Write 도구 실행 후 stdin으로 JSON을 전달함
#
# 최적화: 루트 ktlintFormat은 51개 모듈 전체를 도는 탓에 편집 1회당 ~2.7초가 든다.
# 편집된 파일이 속한 Gradle 프로젝트만 포맷하면 ~1.4초로 줄어든다(설정 비용 ~1.0초가 하한).
# 경로에서 프로젝트를 못 알아내면 루트 전체 포맷으로 폴백한다.

set -euo pipefail

INPUT=$(cat 2>/dev/null || echo '{}')

FILE=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    tool_input = d.get('tool_input', d)
    print(tool_input.get('file_path', ''))
except Exception:
    print('')
" 2>/dev/null || echo '')

# .kt 파일이 아니면 스킵
[[ "$FILE" == *.kt ]] || exit 0

# 프로젝트 루트: Claude Code가 주입하는 $CLAUDE_PROJECT_DIR 우선, 없으면 git 루트로 폴백 (이식성)
PROJECT_ROOT="${CLAUDE_PROJECT_DIR:-$(git -C "$(dirname "$FILE")" rev-parse --show-toplevel 2>/dev/null || echo '')}"
[[ -n "$PROJECT_ROOT" ]] || exit 0
cd "$PROJECT_ROOT" || exit 0

[[ -f "./gradlew" ]] || exit 0

# 파일 경로 → Gradle 프로젝트 경로
#   module-{name}/...                        → :{name}
#   module-{domain}/{domain}-{layer}/...     → :{domain}:{layer}
# (settings.gradle.kts의 projectDir 매핑과 동일한 규칙)
resolve_gradle_project() {
    local rel="${1#"$PROJECT_ROOT"/}"
    local seg1="${rel%%/*}"
    [[ "$seg1" == module-* ]] || return 1

    local base="${seg1#module-}"
    local rest="${rel#"$seg1"/}"
    local seg2="${rest%%/*}"

    if [[ "$seg2" == "$base"-* ]]; then
        printf ':%s:%s' "$base" "${seg2#"$base"-}"
    else
        printf ':%s' "$base"
    fi
}

if TASK=$(resolve_gradle_project "$FILE"); then
    TASK="${TASK}:ktlintFormat"
else
    TASK="ktlintFormat"
fi

if ! OUTPUT=$(./gradlew "$TASK" --quiet 2>&1 | tail -5); then
    echo "⚠️ ktlintFormat 실패($TASK):" >&2
    echo "$OUTPUT" >&2
fi

exit 0
