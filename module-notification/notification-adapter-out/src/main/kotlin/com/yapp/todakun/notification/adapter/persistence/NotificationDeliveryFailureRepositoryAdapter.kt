package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.port.outbound.NotificationDeliveryFailureRepository
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class NotificationDeliveryFailureRepositoryAdapter(
    private val notificationDeliveryFailureJpaRepository: NotificationDeliveryFailureJpaRepository,
) : NotificationDeliveryFailureRepository {
    override fun save(failure: NotificationDeliveryFailure): NotificationDeliveryFailure =
        notificationDeliveryFailureJpaRepository.save(NotificationDeliveryFailureJpaEntity.fromDomain(failure)).toDomain()

    override fun findDue(
        now: Instant,
        limit: Int,
    ): List<NotificationDeliveryFailure> = notificationDeliveryFailureJpaRepository.findDue(now, Limit.of(limit)).map { it.toDomain() }

    override fun deleteById(id: UUID) {
        notificationDeliveryFailureJpaRepository.deleteById(id)
    }
}
