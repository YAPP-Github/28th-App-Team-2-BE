package com.yapp.todakun.yearfortune.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface YearSelectionFortuneJpaRepository : JpaRepository<YearSelectionFortuneJpaEntity, UUID> {
    fun findByMemberIdAndYear(
        memberId: UUID,
        year: Int,
    ): YearSelectionFortuneJpaEntity?

    @Query(value = "SELECT pg_advisory_xact_lock(hashtext(CAST(:memberId AS text)), :year)", nativeQuery = true)
    fun lock(
        @Param("memberId") memberId: UUID,
        @Param("year") year: Int,
    )
}
