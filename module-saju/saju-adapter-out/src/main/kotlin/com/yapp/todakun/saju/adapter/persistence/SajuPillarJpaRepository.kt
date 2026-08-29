package com.yapp.todakun.saju.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SajuPillarJpaRepository : JpaRepository<SajuPillarJpaEntity, UUID> {
    fun findByChartId(chartId: UUID): List<SajuPillarJpaEntity>

    fun deleteByChartIdIn(chartIds: Collection<UUID>)
}
