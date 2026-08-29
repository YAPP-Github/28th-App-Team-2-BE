package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.notification.port.inbound.GetNotificationsUseCase
import com.yapp.todakun.notification.port.inbound.NotificationListResult
import com.yapp.todakun.notification.port.outbound.NotificationRepository
import java.util.UUID

@QueryService
class GetNotificationService(
    private val notificationRepository: NotificationRepository,
) : GetNotificationsUseCase {
    override fun getNotifications(memberId: UUID): NotificationListResult =
        NotificationListResult(
            notifications = notificationRepository.findAllByMemberId(memberId),
            unreadCount = notificationRepository.countUnread(memberId),
        )
}
