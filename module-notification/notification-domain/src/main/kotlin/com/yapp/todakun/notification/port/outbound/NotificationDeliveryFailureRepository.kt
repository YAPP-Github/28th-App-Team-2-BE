package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.NotificationDeliveryFailure
import java.time.Instant
import java.util.UUID

interface NotificationDeliveryFailureRepository {
    fun save(failure: NotificationDeliveryFailure): NotificationDeliveryFailure

    /** [now] 이전에 재시도 예정인 건을 오래된 순으로 최대 [limit]개 조회한다. */
    fun findDue(
        now: Instant,
        limit: Int,
    ): List<NotificationDeliveryFailure>

    fun deleteById(id: UUID)
}
