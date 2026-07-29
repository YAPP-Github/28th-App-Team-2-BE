package com.yapp.todakun.shared

/** 회원 본인(SELF) 사주 명식의 크로스 도메인 축약 뷰. [hourPillar]는 출생 시간을 모르면 null이다. */
data class SajuChartSummary(
    val dayMaster: String,
    val yearPillar: PillarSummary,
    val monthPillar: PillarSummary,
    val dayPillar: PillarSummary,
    val hourPillar: PillarSummary?,
    val ohaeng: Map<String, Int>,
    val sipseong: Map<String, Int>,
)
