package com.yapp.todakun.yearfortune.repository

import com.yapp.todakun.yearfortune.YearSelectionFortune
import java.util.UUID

interface YearSelectionFortuneRepository {
    fun save(yearSelectionFortune: YearSelectionFortune): YearSelectionFortune

    fun findByMemberIdAndYear(
        memberId: UUID,
        year: Int,
    ): YearSelectionFortune?

    /** (memberId, year) 기준 트랜잭션 스코프 DB 락을 건다(커밋·롤백 시 자동 해제). */
    fun lock(
        memberId: UUID,
        year: Int,
    )
}
