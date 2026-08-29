package com.yapp.todakun.compatibility.port.outbound

/**
 * AI가 생성한 궁합 원본 결과(점수·문구). 오행 비율은 결정적 계산이라 여기 포함하지 않는다.
 * SajuCompatibility 저장(도메인 팩토리 호출)은 상위(application)가 담당한다.
 */
data class GeneratedCompatibility(
    val score: Int,
    val headline: String,
    val subheadline: String,
    val summary: String,
    val totalAnalysis: String,
)
