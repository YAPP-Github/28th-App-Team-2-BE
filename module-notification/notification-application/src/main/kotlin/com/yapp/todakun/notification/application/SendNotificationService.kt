package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.Notification
import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.port.outbound.DeviceTokenRepository
import com.yapp.todakun.notification.port.outbound.NotificationRepository
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import com.yapp.todakun.shared.GetPushConsentPort
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import org.springframework.beans.factory.ObjectProvider
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 알림 발급 오케스트레이션. shared.SendNotificationPort의 구현체로, 임의 도메인/스케줄러가 호출한다.
 * 인앱 알림함에는 항상 저장하고, 회원 설정 토글·(야간) 수신 동의를 반영해 FCM 푸시 여부를 결정한다.
 * 만료 토큰은 발송 결과를 보고 정리한다.
 * [pushConsentPort]는 옵셔널 주입 — 구현 빈(terms)이 없으면 "동의"로 간주한다.
 */
@CommandService
class SendNotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
    private val deviceTokenRepository: DeviceTokenRepository,
    private val pushNotificationPort: PushNotificationPort,
    private val pushConsentPort: ObjectProvider<GetPushConsentPort>,
) : SendNotificationPort {
    @ExperimentalUuidApi
    override fun send(command: SendNotificationCommand) {
        val notification =
            notificationRepository.save(
                Notification.create(
                    memberId = command.memberId,
                    type = command.type,
                    title = command.title,
                    content = command.content,
                    deepLink = command.deepLink,
                ),
            )

        if (!command.push || !shouldPush(command)) return

        val tokens = deviceTokenRepository.findAllByMemberId(command.memberId)
        if (tokens.isEmpty()) return

        val results =
            pushNotificationPort.sendAll(
                tokens.map { token ->
                    PushNotification(
                        token = token.token,
                        title = command.title,
                        body = command.content,
                        data = buildData(command, notification.id),
                    )
                },
            )
        results.filter { it.tokenExpired }.forEach { deviceTokenRepository.deleteByToken(it.token) }
    }

    private fun shouldPush(command: SendNotificationCommand): Boolean {
        val setting = notificationSettingRepository.findByMemberId(command.memberId)
        // 설정 미저장 회원: 공지(NOTICE)만 기본 수신, 나머지는 미수신.
        val enabledBySetting = setting?.isPushEnabledFor(command.type) ?: (command.type == NotificationType.NOTICE)
        if (!enabledBySetting) return false
        if (isNight() && pushConsentPort.getIfAvailable()?.isNightPushAllowed(command.memberId) == false) return false
        return true
    }

    private fun isNight(): Boolean {
        val hour = LocalTime.now(SEOUL_ZONE).hour
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
    }

    private fun buildData(
        command: SendNotificationCommand,
        notificationId: UUID,
    ): Map<String, String> =
        buildMap {
            put("type", command.type.name)
            put("notificationId", notificationId.toString())
            command.deepLink?.let { put("deepLink", it) }
        }

    companion object {
        private val SEOUL_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private const val NIGHT_START_HOUR = 21
        private const val NIGHT_END_HOUR = 8
    }
}
