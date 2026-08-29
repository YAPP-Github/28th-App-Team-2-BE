package com.yapp.todakun.shared

/** 회원 본인(SELF) 사주 명식의 크로스 도메인 축약 뷰. [hourPillar]는 시주 없이 저장된 과거 명식에서만 null이다(시간 모름은 00:00 기준으로 계산해 채운다). */
data class SajuChartSummary(
    val dayMaster: String,
    val yearPillar: PillarSummary,
    val monthPillar: PillarSummary,
    val dayPillar: PillarSummary,
    val hourPillar: PillarSummary?,
    val ohaeng: Map<String, Int>,
    val sipseong: Map<String, Int>,
)
