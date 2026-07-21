package com.yapp.todakun.notification.adapter.web.dto.response

import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.port.inbound.NotificationListResult
import com.yapp.todakun.shared.NotificationType
import java.time.Instant
import java.util.UUID

data class NotificationListResponse(
    val unreadCount: Long,
    val notifications: List<NotificationResponse>,
) {
    companion object {
        fun from(result: NotificationListResult) =
            NotificationListResponse(
                unreadCount = result.unreadCount,
                notifications = result.notifications.map(NotificationResponse::from),
            )
    }
}

data class NotificationResponse(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val content: String,
    val deepLink: String?,
    val isRead: Boolean,
    val createdAt: Instant?,
) {
    companion object {
        fun from(notification: Notification) =
            NotificationResponse(
                id = notification.id,
                type = notification.type,
                title = notification.title,
                content = notification.content,
                deepLink = notification.deepLink,
                isRead = notification.isRead,
                createdAt = notification.createdAt,
            )
    }
}
