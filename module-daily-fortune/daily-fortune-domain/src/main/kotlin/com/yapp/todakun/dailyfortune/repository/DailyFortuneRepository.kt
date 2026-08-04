package com.yapp.todakun.dailyfortune.repository

import com.yapp.todakun.dailyfortune.DailyFortune
import java.time.LocalDate
import java.util.UUID

interface DailyFortuneRepository {
    fun save(dailyFortune: DailyFortune): DailyFortune

    fun findById(id: UUID): DailyFortune?

    fun findByMemberIdAndFortuneDate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortune?

    fun findAllByMemberIdBetweenFortuneDates(
        memberId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyFortune>

    /** (memberId, fortuneDate) 기준 트랜잭션 스코프 DB 락을 건다(커밋·롤백 시 자동 해제). 동시 생성 요청을 직렬화해 유니크 제약 충돌을 막는다. */
    fun lock(
        memberId: UUID,
        fortuneDate: LocalDate,
    )
}
