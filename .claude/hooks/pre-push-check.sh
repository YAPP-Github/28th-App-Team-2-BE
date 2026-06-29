#!/usr/bin/env bash
# PreToolUse(Bash) hook: `git push` 직전 코드 스타일 + 아키텍처 규칙을 강제한다.
# 실패 시 exit 2로 push를 차단하고 사유를 stderr로 알린다.
#
# 게이트 범위: ktlintCheck(스타일) + :architecture-test:test(Konsist).
#   전체 test는 느려서 게이트에서 제외 — 필요하면 PUSH_GATE_RUN_TESTS=1로 켠다.

INPUT=$(cat 2>/dev/null || echo '{}')
COMMAND=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    print(json.load(sys.stdin).get('tool_input', {}).get('command', ''))
except Exception:
    print('')
" 2>/dev/null || echo '')

# git push 가 아니면 통과
[[ "$COMMAND" =~ (^|[[:space:]&|;])git[[:space:]]+push ]] || exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
[[ -f "./gradlew" ]] || exit 0

if ! ./gradlew ktlintCheck --quiet 2>&1; then
    echo "❌ ktlintCheck 실패. './gradlew ktlintFormat' 로 정리 후 다시 커밋하고 push하세요." >&2
    exit 2
fi

if ! ./gradlew :architecture-test:test --quiet 2>&1; then
    echo "❌ 아키텍처 규칙(Konsist) 위반. 레이어 의존/네이밍 규칙을 확인하세요." >&2
    exit 2
fi

if [[ "${PUSH_GATE_RUN_TESTS:-0}" == "1" ]]; then
    if ! ./gradlew test --quiet 2>&1; then
        echo "❌ 테스트 실패. 테스트를 통과시킨 후 다시 push하세요." >&2
        exit 2
    fi
fi

exit 0