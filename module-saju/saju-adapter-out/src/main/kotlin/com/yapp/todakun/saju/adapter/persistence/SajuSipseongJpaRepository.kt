package com.yapp.todakun.saju.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SajuSipseongJpaRepository : JpaRepository<SajuSipseongJpaEntity, UUID> {
    fun findByChartId(chartId: UUID): List<SajuSipseongJpaEntity>

    fun deleteByChartIdIn(chartIds: Collection<UUID>)
}
