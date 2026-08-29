package com.yapp.todakun.notification.adapter.persistence

import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface NotificationDeliveryFailureJpaRepository : JpaRepository<NotificationDeliveryFailureJpaEntity, UUID> {
    @Query("select f from NotificationDeliveryFailureJpaEntity f where f.nextRetryAt <= :now order by f.nextRetryAt asc")
    fun findDue(
        now: Instant,
        limit: Limit,
    ): List<NotificationDeliveryFailureJpaEntity>
}
