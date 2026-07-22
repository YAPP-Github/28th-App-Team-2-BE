package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.Notification
import java.util.UUID

interface NotificationRepository {
    fun save(notification: Notification): Notification

    fun findById(id: UUID): Notification?

    fun findAllByMemberId(memberId: UUID): List<Notification>

    fun countUnread(memberId: UUID): Long

    fun markAllReadByMemberId(memberId: UUID): Int
}
