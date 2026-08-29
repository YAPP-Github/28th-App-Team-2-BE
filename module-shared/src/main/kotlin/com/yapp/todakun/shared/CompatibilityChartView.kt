package com.yapp.todakun.shared

/**
 * 궁합 입력용 명식 한 벌의 크로스 도메인 축약 뷰. [ohaeng]은 오행 코드(WOOD/FIRE/EARTH/METAL/WATER)별 글자 수,
 * [sipseong]은 십성 라벨별 개수. [hourPillar]는 시주 없이 저장된 과거 명식에서만 null이다(시간 모름은 00:00 기준으로 계산해 채운다).
 */
data class CompatibilityChartView(
    val dayMaster: String,
    val yearPillar: PillarSummary,
    val monthPillar: PillarSummary,
    val dayPillar: PillarSummary,
    val hourPillar: PillarSummary?,
    val ohaeng: Map<String, Int>,
    val sipseong: Map<String, Int>,
)
