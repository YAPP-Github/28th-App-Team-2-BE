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
}
