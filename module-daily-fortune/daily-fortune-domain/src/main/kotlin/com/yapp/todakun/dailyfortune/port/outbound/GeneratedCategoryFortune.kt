package com.yapp.todakun.dailyfortune.port.outbound

import com.yapp.todakun.shared.FortuneCategory

/**
 * AI가 생성한 카테고리별(관계/연애/성취/금전/건강) 운세 한 건을 담는다.
 * LuckAction 저장은 상위(application)가 담당한다.
 */
data class GeneratedCategoryFortune(
    val fortuneCategory: FortuneCategory,
    val score: Int,
    val title: String,
    val content: String,
)
