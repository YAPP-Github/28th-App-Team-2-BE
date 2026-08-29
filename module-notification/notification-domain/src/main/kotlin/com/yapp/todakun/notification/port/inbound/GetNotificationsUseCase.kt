package com.yapp.todakun.notification.port.inbound

import java.util.UUID

interface GetNotificationsUseCase {
    fun getNotifications(memberId: UUID): NotificationListResult
}
