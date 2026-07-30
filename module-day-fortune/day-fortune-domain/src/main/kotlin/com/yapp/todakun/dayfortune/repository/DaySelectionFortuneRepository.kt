package com.yapp.todakun.dayfortune.repository

import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import java.time.LocalDate
import java.util.UUID

interface DaySelectionFortuneRepository {
    fun save(daySelectionFortune: DaySelectionFortune): DaySelectionFortune

    fun findByMemberIdAndPurposeAndTargetDate(
        memberId: UUID,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
    ): DaySelectionFortune?

    /** (memberId, purpose, targetDate) 기준 트랜잭션 스코프 DB 락을 건다(커밋·롤백 시 자동 해제). */
    fun lock(
        memberId: UUID,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
    )
}
