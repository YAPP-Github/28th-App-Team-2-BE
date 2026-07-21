package com.yapp.todakun.notification.adapter.persistence

import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.port.outbound.NotificationRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NotificationRepositoryAdapter(
    private val notificationJpaRepository: NotificationJpaRepository,
) : NotificationRepository {
    override fun save(notification: Notification): Notification =
        notificationJpaRepository.save(NotificationJpaEntity.fromDomain(notification)).toDomain()

    override fun findById(id: UUID): Notification? = notificationJpaRepository.findById(id).map { it.toDomain() }.orElse(null)

    override fun findAllByMemberId(memberId: UUID): List<Notification> =
        notificationJpaRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId).map { it.toDomain() }

    override fun countUnread(memberId: UUID): Long = notificationJpaRepository.countUnread(memberId)

    override fun markAllReadByMemberId(memberId: UUID): Int = notificationJpaRepository.markAllRead(memberId)
}
