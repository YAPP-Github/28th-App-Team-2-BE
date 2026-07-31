package com.yapp.todakun.dayfortune.adapter.persistence

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface DaySelectionFortuneJpaRepository : JpaRepository<DaySelectionFortuneJpaEntity, UUID> {
    fun findByMemberIdAndPurposeAndTargetDate(
        memberId: UUID,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
    ): DaySelectionFortuneJpaEntity?

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtext(CAST(:memberId AS text)), hashtext(:purpose || CAST(:targetDate AS text)))",
        nativeQuery = true,
    )
    fun lock(
        @Param("memberId") memberId: UUID,
        @Param("purpose") purpose: String,
        @Param("targetDate") targetDate: LocalDate,
    )
}
