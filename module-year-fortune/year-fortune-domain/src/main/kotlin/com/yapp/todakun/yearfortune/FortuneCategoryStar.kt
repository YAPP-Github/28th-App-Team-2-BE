package com.yapp.todakun.yearfortune

import com.yapp.todakun.shared.FortuneCategory

/** 연도 선택 운세에서 특정 [fortuneCategory]에 매긴 별점([star], 1~3점). */
data class FortuneCategoryStar(
    val fortuneCategory: FortuneCategory,
    val star: Int,
)
