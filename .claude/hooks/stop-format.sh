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
#
# 최적화: 파일마다 tail+od+tr 3개 프로세스를 띄우면 추적 파일 1,000개 기준 ~2.4초가 든다.
# perl 한 패스로 마지막 바이트만 seek해서 읽으면 ~0.13초로 줄어든다(약 19배).
git ls-files -z | perl -0ne '
    chomp;
    my $path = $_;
    next unless -f $path && -s $path;

    # 바이너리 확장자 제외 (ConventionTest의 binaryExtensions와 동일하게 유지할 것)
    next if $path =~ /\.(jar|png|jpe?g|gif|ico|svg|woff2?|ttf|class|keystore|p12)$/i;

    # 검사는 읽기 전용으로 연다. "+<"로 열면 읽기 단계부터 쓰기 권한을 요구해,
    # 읽기 전용 파일은 조용히 건너뛰고 훅은 성공한 채 ConventionTest만 나중에 실패한다.
    open(my $fh, "<", $path) or do { warn "EOF 검사 실패(열기): $path: $!\n"; next };
    binmode $fh;
    seek($fh, -1, 2) or do { warn "EOF 검사 실패(seek): $path: $!\n"; close $fh; next };
    read($fh, my $last, 1);
    close $fh;
    next if $last eq "\n";

    # 개행이 없을 때만 append 모드로 다시 연다. 실패는 숨기지 않고 알린다.
    open(my $out, ">>", $path) or do { warn "개행 문자 추가 실패(열기): $path: $!\n"; next };
    binmode $out;
    print $out "\n" or warn "개행 문자 추가 실패(쓰기): $path: $!\n";
    close $out or warn "개행 문자 추가 실패(닫기): $path: $!\n";
    print "개행 문자 추가: $path\n";
'

exit 0
