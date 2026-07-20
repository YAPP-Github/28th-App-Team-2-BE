package com.yapp.todakun.member

/** 회원 탈퇴 사유. 탈퇴 후에도 통계용으로 로그에 보관한다([label]은 화면 표시용). */
enum class WithdrawalReason(
    val label: String,
) {
    PRIVACY_CONCERN("개인정보 유출이 우려돼요"),
    TOO_MANY_NOTIFICATIONS("알림이 너무 많아요"),
    LACK_OF_CONTENT("콘텐츠가 부족해요"),
    NOT_USING("자주 사용하지 않아요"),
    ETC("기타"),
}
