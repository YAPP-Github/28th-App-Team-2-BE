package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.DeviceToken
import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.port.outbound.DeviceTokenRepository
import com.yapp.todakun.notification.port.outbound.NotificationDeliveryFailureRepository
import com.yapp.todakun.notification.port.outbound.NotificationRepository
import java.time.Instant
import java.util.UUID

/**
 * 알림 발송의 짧은 DB 트랜잭션 경계를 소유하는 협력 빈.
 * FCM 호출(외부 I/O)은 이 빈 밖(오케스트레이터인 [SendNotificationService]/[RetryFailedNotificationsService])에서
 * 실행해, DB 커넥션을 FCM 네트워크 I/O 동안 점유하지 않게 한다(#41과 동일 원칙).
 */
@CommandService
class NotificationTransactionalStore(
    private val notificationRepository: NotificationRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val notificationDeliveryFailureRepository: NotificationDeliveryFailureRepository,
) {
    fun saveNotification(notification: Notification): Notification = notificationRepository.save(notification)

    fun getDeviceTokens(memberId: UUID): List<DeviceToken> = deviceTokenRepository.findAllByMemberId(memberId)

    /** 발송 결과 중 만료된(tokenExpired) 토큰만 정리한다. */
    fun cleanupExpiredTokens(results: List<PushResult>) {
        results.filter { it.tokenExpired }.forEach { deviceTokenRepository.deleteByToken(it.token) }
    }

    fun saveDeliveryFailure(failure: NotificationDeliveryFailure): NotificationDeliveryFailure =
        notificationDeliveryFailureRepository.save(failure)

    fun findDueDeliveryFailures(
        now: Instant,
        limit: Int,
    ): List<NotificationDeliveryFailure> = notificationDeliveryFailureRepository.findDue(now, limit)

    fun deleteDeliveryFailure(id: UUID) = notificationDeliveryFailureRepository.deleteById(id)
}
