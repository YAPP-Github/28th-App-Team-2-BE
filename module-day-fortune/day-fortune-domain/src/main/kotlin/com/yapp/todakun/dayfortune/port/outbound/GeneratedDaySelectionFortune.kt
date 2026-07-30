package com.yapp.todakun.dayfortune.port.outbound

/**
 * AI가 생성한 택일 운세 원본 결과를 담는다.
 * DaySelectionFortune 저장(도메인 팩토리 호출)은 상위(application)가 담당한다.
 */
data class GeneratedDaySelectionFortune(
    val title: String,
    val content: String,
    val score: Int,
    val fortuneCategories: List<GeneratedCategoryFortune>,
)
