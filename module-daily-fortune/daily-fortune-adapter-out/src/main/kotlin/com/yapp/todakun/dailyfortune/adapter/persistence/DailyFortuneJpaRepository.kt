package com.yapp.todakun.dailyfortune.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface DailyFortuneJpaRepository : JpaRepository<DailyFortuneJpaEntity, UUID> {
    fun findByMemberIdAndFortuneDate(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortuneJpaEntity?

    fun findAllByMemberIdAndFortuneDateBetweenOrderByFortuneDateAsc(
        memberId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyFortuneJpaEntity>
}
