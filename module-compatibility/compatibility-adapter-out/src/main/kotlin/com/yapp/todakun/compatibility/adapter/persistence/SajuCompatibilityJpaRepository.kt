package com.yapp.todakun.compatibility.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SajuCompatibilityJpaRepository : JpaRepository<SajuCompatibilityJpaEntity, UUID> {
    fun findByMemberIdAndMyChartIdAndPartnerChartId(
        memberId: UUID,
        myChartId: UUID,
        partnerChartId: UUID,
    ): SajuCompatibilityJpaEntity?

    @Query(
        value = "SELECT pg_advisory_xact_lock(hashtext(CAST(:myChartId AS text)), hashtext(CAST(:partnerChartId AS text)))",
        nativeQuery = true,
    )
    fun lock(
        @Param("myChartId") myChartId: UUID,
        @Param("partnerChartId") partnerChartId: UUID,
    )
}
