package com.yapp.todakun.dayfortune

/** 회원이 택일 운세를 조회하는 목적. */
enum class DaySelectionPurpose(
    val label: String,
) {
    CONTRACT_MOVING("계약·이사"),
    BUSINESS_OPENING("개업"),
    TRAVEL("여행"),
    CONFESSION_DATING("고백·소개팅"),
    EXAM_INTERVIEW("시험·면접"),
}
