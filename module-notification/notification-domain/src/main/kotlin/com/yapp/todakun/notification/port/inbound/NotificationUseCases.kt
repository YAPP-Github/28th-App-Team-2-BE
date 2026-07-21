package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.Notification
import java.util.UUID

interface GetNotificationsUseCase {
    fun getNotifications(memberId: UUID): NotificationListResult
}

interface ReadNotificationUseCase {
    fun read(
        memberId: UUID,
        notificationId: UUID,
    )

    fun readAll(memberId: UUID)
}

data class NotificationListResult(
    val notifications: List<Notification>,
    val unreadCount: Long,
)
