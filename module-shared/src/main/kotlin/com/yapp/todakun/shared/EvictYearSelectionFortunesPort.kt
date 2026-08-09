package com.yapp.todakun.shared

import java.util.UUID

/**
 * 회원의 YEAR_FORTUNE 캐시를 모두 비우는 크로스 도메인 포트.
 * 연도별 운세는 사주 명식(dayMaster/오행/십성 등)을 기반으로 생성되므로,
 * 사주가 바뀌거나 삭제되면 saju가 year-fortune 내부 구조를 알지 않고, 이 포트로 캐시 무효화를 위임한다(year-fortune-application 구현).
 *
 */
interface EvictYearSelectionFortunesPort {
    fun evictByMemberId(memberId: UUID)
}
