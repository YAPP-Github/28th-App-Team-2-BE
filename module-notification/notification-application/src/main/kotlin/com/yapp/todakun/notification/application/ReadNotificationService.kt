package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.exception.NotificationAccessDeniedException
import com.yapp.todakun.notification.exception.NotificationNotFoundException
import com.yapp.todakun.notification.port.inbound.ReadNotificationUseCase
import com.yapp.todakun.notification.port.outbound.NotificationRepository
import java.util.UUID

@CommandService
class ReadNotificationService(
    private val notificationRepository: NotificationRepository,
) : ReadNotificationUseCase {
    override fun read(
        memberId: UUID,
        notificationId: UUID,
    ) {
        val notification = notificationRepository.findById(notificationId) ?: throw NotificationNotFoundException()
        if (notification.memberId != memberId) throw NotificationAccessDeniedException()
        notificationRepository.save(notification.markAsRead())
    }

    override fun readAll(memberId: UUID) {
        notificationRepository.markAllReadByMemberId(memberId)
    }
}
