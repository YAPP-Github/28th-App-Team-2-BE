package com.yapp.todakun.compatibility.port.outbound

/**
 * 궁합 AI 프롬프트에 넣을 명식 한 벌. member/saju 도메인 타입을 직접 참조하지 않도록 원시 타입으로만 구성한다.
 * [ohaeng]은 오행 코드별 글자 수, [sipseong]은 십성 라벨별 개수. [hourPillar]는 시주 없이 저장된 과거 명식에서만 null이다(시간 모름은 00:00 기준으로 계산해 채운다).
 */
data class CompatibilityChartProfile(
    val dayMaster: String,
    val yearPillar: CompatibilityPillar,
    val monthPillar: CompatibilityPillar,
    val dayPillar: CompatibilityPillar,
    val hourPillar: CompatibilityPillar?,
    val ohaeng: Map<String, Int>,
    val sipseong: Map<String, Int>,
)
