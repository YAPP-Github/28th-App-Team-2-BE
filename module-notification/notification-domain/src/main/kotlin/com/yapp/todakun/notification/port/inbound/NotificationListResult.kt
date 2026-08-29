package com.yapp.todakun.notification.port.inbound

import com.yapp.todakun.notification.Notification

data class NotificationListResult(
    val notifications: List<Notification>,
    val unreadCount: Long,
)
