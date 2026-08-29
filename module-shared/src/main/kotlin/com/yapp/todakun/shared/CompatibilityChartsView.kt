package com.yapp.todakun.shared

import java.util.UUID

/**
 * 궁합 생성 입력용 두 명식 뷰. 본인(SELF)·상대(PARTNER) 명식과 상대 이름·관계 라벨(스냅샷 원본)을 함께 담는다.
 * [relationshipType]은 상대 링크의 관계 라벨 코드(LOVER/FRIEND/...)이며, 궁합 레코드에 스냅샷으로 복사된다.
 */
data class CompatibilityChartsView(
    val myChartId: UUID,
    val partnerChartId: UUID,
    val partnerName: String?,
    val relationshipType: String,
    val myChart: CompatibilityChartView,
    val partnerChart: CompatibilityChartView,
)
