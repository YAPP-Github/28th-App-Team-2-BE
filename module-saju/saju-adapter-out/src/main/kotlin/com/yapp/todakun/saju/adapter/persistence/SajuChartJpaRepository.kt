package com.yapp.todakun.saju.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SajuChartJpaRepository : JpaRepository<SajuChartJpaEntity, UUID>
