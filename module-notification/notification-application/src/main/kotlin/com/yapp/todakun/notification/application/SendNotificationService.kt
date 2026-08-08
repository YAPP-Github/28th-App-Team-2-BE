package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.NotificationDeliveryFailure
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.policy.NotificationRetryPolicy
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import com.yapp.todakun.shared.GetPushConsentPort
import com.yapp.todakun.shared.NotificationClassification
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import kotlin.uuid.ExperimentalUuidApi

/**
 * 알림 발급 오케스트레이션. shared.SendNotificationPort의 구현체로, 임의 도메인/스케줄러가 호출한다.
 * 인앱 알림함에는 항상 저장하고, 회원 설정 토글·(야간) 수신 동의를 반영해 FCM 푸시 여부를 결정한다.
 * 만료 토큰은 발송 결과를 보고 정리하고, 그 외 일시적 실패(네트워크·FCM 오류)는 [NotificationRetryPolicy]에
 * 따라 재시도 대기열([NotificationDeliveryFailure])에 등록한다 — 실제 재시도는 [RetryFailedNotificationsService]가 수행한다.
 * [pushConsentPort]는 옵셔널 주입 — 구현 빈(terms)이 없으면 "동의"로 간주한다.
 * 트랜잭션을 걸지 않는다 — FCM 호출(외부 I/O)을 DB 트랜잭션 밖에서 수행하기 위함이다(#41과 동일 원칙).
 * DB 저장/조회는 [NotificationTransactionalStore]의 독립 트랜잭션으로 위임한다.
 */
@Service
class SendNotificationService(
    private val notificationTransactionalStore: NotificationTransactionalStore,
    private val notificationSettingRepository: NotificationSettingRepository,
    private val pushNotificationPort: PushNotificationPort,
    private val pushConsentPort: ObjectProvider<GetPushConsentPort>,
    private val notificationMetrics: NotificationMetrics,
) : SendNotificationPort {
    @ExperimentalUuidApi
    override fun send(command: SendNotificationCommand) {
        val notification =
            notificationTransactionalStore.saveNotification(
                Notification.create(
                    memberId = command.memberId,
                    type = command.type,
                    title = command.title,
                    content = command.content,
                    deepLink = command.deepLink,
                ),
            )

        if (!command.push || !shouldPush(command)) return

        val tokens = notificationTransactionalStore.getDeviceTokens(command.memberId)
        if (tokens.isEmpty()) return

        val results =
            pushNotificationPort.sendAll(
                tokens.map { token ->
                    PushNotification(
                        token = token.token,
                        title = command.title,
                        body = command.content,
                        data = buildPushData(command.type, notification.id, command.deepLink),
                    )
                },
            )
        notificationTransactionalStore.cleanupExpiredTokens(results)

        if (results.any { !it.success && !it.tokenExpired }) {
            notificationMetrics.record(command.type, NotificationDispatchResult.FAILURE)
            notificationTransactionalStore.saveDeliveryFailure(
                NotificationDeliveryFailure.create(
                    memberId = command.memberId,
                    notificationId = notification.id,
                    type = command.type,
                    title = command.title,
                    content = command.content,
                    deepLink = command.deepLink,
                    nextRetryAt = Instant.now().plus(NotificationRetryPolicy.backoffFor(0)),
                ),
            )
        } else {
            notificationMetrics.record(command.type, NotificationDispatchResult.SUCCESS)
        }
    }

    private fun shouldPush(command: SendNotificationCommand): Boolean {
        val setting = notificationSettingRepository.findByMemberId(command.memberId)
        // OS 알림 권한이 꺼진 것으로 동기화돼 있으면 발송 전 스킵(notification.md 6절). null(미동기화)이면 통과.
        if (setting?.osPushPermission == false) return false
        // 설정 미저장 회원: 공지(NOTICE)만 기본 수신, 나머지는 미수신.
        val enabledBySetting = setting?.isPushEnabledFor(command.type) ?: (command.type == NotificationType.NOTICE)
        if (!enabledBySetting) return false
        // SERVICE는 야간에도 무조건 발송(notification.md 4절) — 야간 동의는 MARKETING에만 적용.
        if (command.type.classification != NotificationClassification.MARKETING) return true
        if (isNight() && pushConsentPort.getIfAvailable()?.isNightPushAllowed(command.memberId) == false) return false
        return true
    }

    private fun isNight(): Boolean {
        val hour = LocalTime.now(SEOUL_ZONE).hour
        // 주간(08~20시)의 여집합 = 야간. 두 비교를 range 부정 체크 하나로 합친다.
        return hour !in NIGHT_END_HOUR until NIGHT_START_HOUR
    }
}

private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
private const val NIGHT_START_HOUR = 21
private const val NIGHT_END_HOUR = 8
