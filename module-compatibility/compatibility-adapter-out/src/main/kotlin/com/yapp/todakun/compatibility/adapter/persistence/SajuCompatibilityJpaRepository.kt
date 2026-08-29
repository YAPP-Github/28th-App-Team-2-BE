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

    // 두 명식 키를 least/greatest로 정렬해 (a,b)·(b,a) 요청이 동일한 락을 잡도록 한다(락 키 순서 무관하게 직렬화).
    @Query(
        value =
            "SELECT pg_advisory_xact_lock(" +
                "least(hashtext(CAST(:myChartId AS text)), hashtext(CAST(:partnerChartId AS text))), " +
                "greatest(hashtext(CAST(:myChartId AS text)), hashtext(CAST(:partnerChartId AS text))))",
        nativeQuery = true,
    )
    fun lock(
        @Param("myChartId") myChartId: UUID,
        @Param("partnerChartId") partnerChartId: UUID,
    )
}
