#!/usr/bin/env bash
# PostToolUse hook: .kt 파일 수정 시 ktlint 자동 포맷
# Claude Code가 Edit/Write 도구 실행 후 stdin으로 JSON을 전달함

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

./gradlew ktlintFormat --quiet 2>&1 | tail -5 || true
