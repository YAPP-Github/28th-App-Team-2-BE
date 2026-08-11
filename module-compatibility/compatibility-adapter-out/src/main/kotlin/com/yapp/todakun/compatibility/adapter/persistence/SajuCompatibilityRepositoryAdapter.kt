package com.yapp.todakun.compatibility.adapter.persistence

import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class SajuCompatibilityRepositoryAdapter(
    private val sajuCompatibilityJpaRepository: SajuCompatibilityJpaRepository,
) : SajuCompatibilityRepository {
    override fun save(compatibility: SajuCompatibility): SajuCompatibility =
        sajuCompatibilityJpaRepository.save(SajuCompatibilityJpaEntity.fromDomain(compatibility)).toDomain()

    override fun findById(id: UUID): SajuCompatibility? = sajuCompatibilityJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findByMemberIdAndCharts(
        memberId: UUID,
        myChartId: UUID,
        partnerChartId: UUID,
    ): SajuCompatibility? =
        sajuCompatibilityJpaRepository
            .findByMemberIdAndMyChartIdAndPartnerChartId(memberId, myChartId, partnerChartId)
            ?.toDomain()

    override fun lock(
        myChartId: UUID,
        partnerChartId: UUID,
    ) = sajuCompatibilityJpaRepository.lock(myChartId, partnerChartId)
}
