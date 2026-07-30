package com.yapp.todakun.shared

import java.util.UUID

/**
 * 궁합(compatibility 도메인)이 두 사주 명식을 조합해 AI 해석을 생성할 때, 회원 본인(SELF)과 선택한 상대(PARTNER) 명식을 함께 조회하는 확장점.
 * compatibility 도메인이 saju 도메인과 직접 결합하지 않도록 이 포트를 거친다. 상대 링크가 회원의 것인지 소유권 검증은 구현체(saju)가 담당한다.
 */
interface GetSajuChartsForCompatibilityPort {
    fun getCharts(
        memberId: UUID,
        partnerLinkId: UUID,
    ): CompatibilityChartsView
}
