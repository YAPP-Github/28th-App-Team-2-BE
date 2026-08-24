package com.yapp.todakun.common.format

/**
 * 사주 명식 한 기둥(간지)을 "갑자 (천간 비견, 지지 정관, 십이운성 제왕)" 형식으로 포맷한다.
 * 일주 천간처럼 [stemSipseong]이 없는 경우 천간 표기를 생략한다.
 */
fun formatSajuPillar(
    stem: String,
    branch: String,
    stemSipseong: String?,
    branchSipseong: String,
    sibiunseong: String,
): String {
    val stemPart = stemSipseong?.let { "천간 $it, " } ?: ""

    return "$stem$branch (${stemPart}지지 $branchSipseong, 십이운성 $sibiunseong)"
}
