package com.yapp.todakun.yearfortune.port.outbound

/**
 * AI가 생성한 연도별 운세 원본 결과를 담는다.
 * YearSelectionFortune 저장(도메인 팩토리 호출)은 상위(application)가 담당한다.
 */
data class GeneratedYearSelectionFortune(
    val title: String,
    val content: String,
    val score: Int,
    val fortuneCategories: List<GeneratedCategoryFortune>,
)
