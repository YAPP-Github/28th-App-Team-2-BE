#!/usr/bin/env bash
# PR 전 전체 검증 스크립트
# 실행: ./.claude/scripts/check-all.sh

set -euo pipefail

ROOT="/Users/tisckd/Documents/code/yapp/28th-App-Team-2-BE"
cd "$ROOT"

echo "========================================"
echo "  PR 전 전체 검증"
echo "========================================"

echo ""
echo "[1/4] ktlint 자동 수정..."
./gradlew ktlintFormat --quiet
echo "✓ ktlint format 완료"

echo ""
echo "[2/4] ktlint 스타일 검증..."
./gradlew ktlintCheck
echo "✓ ktlint check 통과"

echo ""
echo "[3/4] Konsist 아키텍처 규칙 검증..."
./gradlew :architecture-test:test
echo "✓ 아키텍처 규칙 통과"

echo ""
echo "[4/4] 전체 테스트..."
./gradlew test
echo "✓ 테스트 통과"

echo ""
echo "========================================"
echo "  모든 검증 통과! PR 준비 완료."
echo "========================================"
