package com.yapp.todakun.saju.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SajuOhaengJpaRepository : JpaRepository<SajuOhaengJpaEntity, UUID> {
    fun findByChartId(chartId: UUID): List<SajuOhaengJpaEntity>

    fun deleteByChartIdIn(chartIds: Collection<UUID>)
}
