package com.yapp.todakun.notification.port.inbound

import java.util.UUID

interface ReadNotificationUseCase {
    fun read(
        memberId: UUID,
        notificationId: UUID,
    )

    fun readAll(memberId: UUID)
}
