package com.yapp.todakun.saju.port.outbound

/**
 * 만세력 계산 결과인 사주 4주(년/월/일/시)의 간지. 십성·십이운성은 이 값으로 도메인이 파생 계산한다.
 * 시간 모름도 00:00(자시)로 계산하므로(BirthTime.calculationBranch) [hour]는 항상 채워진다 — 계산 결과는 언제나 4주다.
 * [solarTermName]은 월주 산정에 적용된 절기명 스냅샷.
 */
data class FourPillars(
    val year: GanjiPillar,
    val month: GanjiPillar,
    val day: GanjiPillar,
    val hour: GanjiPillar,
    val solarTermName: String?,
)
