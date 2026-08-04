package com.yapp.todakun.dailyfortune.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
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

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtext(CAST(:memberId AS text)), hashtext(CAST(:fortuneDate AS text)))",
        nativeQuery = true,
    )
    fun lock(
        @Param("memberId") memberId: UUID,
        @Param("fortuneDate") fortuneDate: LocalDate,
    )
}
