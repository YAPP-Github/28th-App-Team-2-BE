package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

/**
 * 오늘의 운세(fortune 도메인)가 특정 회원·기간의 행운 액션 목록(제목·달성 여부 포함)을 조회하는 확장점.
 * "오늘의 운세" 도메인이 luck 도메인과 직접 결합하지 않도록 이 포트를 거친다.
 */
interface GetLuckActionSummariesPort {
    fun getSummaries(
        memberId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<LuckActionSummary>
}
