package com.yapp.todakun.shared

/** 운세 카테고리. [label] 한글 표기. */
enum class FortuneCategory(
    val label: String,
) {
    RELATIONSHIP("관계운"),
    LOVE("연애운"),
    ACHIEVEMENT("성취운"),
    MONEY("금전운"),
    HEALTH("건강운"),
}
