package com.yapp.todakun.compatibility

/**
 * 궁합 오행 시각화에 쓰는 오행(五行). saju 도메인의 Element를 직접 참조하지 않도록 궁합 도메인이 독립 정의한다.
 * [label]은 화면 표시용 한글, [hanja]는 한자. 두 명식의 오행을 합산·정규화한 비율(%)을 이 5개로 표현한다.
 */
enum class CompatibilityElement(
    val label: String,
    val hanja: String,
) {
    WOOD("목", "木"),
    FIRE("화", "火"),
    EARTH("토", "土"),
    METAL("금", "金"),
    WATER("수", "水"),
}
