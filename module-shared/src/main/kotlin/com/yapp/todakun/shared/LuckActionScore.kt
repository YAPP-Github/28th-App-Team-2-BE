package com.yapp.todakun.shared

import java.util.UUID

/** 오늘의 운세 화면에서 행운 액션·관심 운세와 연동해 보여줄 카테고리별 점수 한 건. */
data class LuckActionScore(
    val id: UUID,
    val fortuneCategory: FortuneCategory,
    val score: Int,
)
