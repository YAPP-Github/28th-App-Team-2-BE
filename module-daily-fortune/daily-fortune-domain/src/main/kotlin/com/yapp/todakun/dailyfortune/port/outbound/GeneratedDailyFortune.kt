package com.yapp.todakun.dailyfortune.port.outbound

/**
 * AI가 생성한 오늘의 운세 원본 결과를 담는다.
 * DailyFortune 저장(도메인 팩토리 호출)은 상위(application)가 담당한다.
 */
data class GeneratedDailyFortune(
    val title: String,
    val content: String,
    val luckyItems: List<String>,
    val cautionaryItems: List<String>,
    val categoryFortunes: List<GeneratedCategoryFortune>,
)
