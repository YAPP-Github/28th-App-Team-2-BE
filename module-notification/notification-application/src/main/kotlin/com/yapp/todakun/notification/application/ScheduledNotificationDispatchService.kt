package com.yapp.todakun.notification.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.notification.port.inbound.DispatchScheduledNotificationUseCase
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.shared.DailyFortuneNotificationPort
import com.yapp.todakun.shared.LuckyActionNotificationPort
import com.yapp.todakun.shared.NotificationPayload
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import java.time.LocalTime
import java.util.UUID

/**
 * 스케줄 알림(아침 운 리포트/행운 액션 리마인드) 대상 선별·콘텐츠 조달·발송.
 * 콘텐츠 확장 포트([dailyFortunePort]/[luckyActionPort])는 옵셔널 주입 — 구현 빈이 없으면 기본 문구를 사용한다.
 * 소스 도메인이 생기면 같은 포트를 구현하는 빈을 추가하면 자동으로 실제 콘텐츠로 대체된다.
 * 대상 회원별 발송은 예외를 격리해 한 명의 실패가 나머지 회원의 발송을 중단시키지 않는다.
 * OutOfMemoryError 등 복구 불가능한 [Error]는 격리 대상이 아니므로 [Exception]만 명시적으로 잡아 전파시킨다.
 */
@CommandService
class ScheduledNotificationDispatchService(
    private val notificationSettingRepository: NotificationSettingRepository,
    private val sendNotificationPort: SendNotificationPort,
    private val dailyFortunePort: ObjectProvider<DailyFortuneNotificationPort>,
    private val luckyActionPort: ObjectProvider<LuckyActionNotificationPort>,
) : DispatchScheduledNotificationUseCase {
    override fun dispatchMorningReport(slot: LocalTime) {
        notificationSettingRepository.findAllMorningReportTargets(slot).forEach { setting ->
            try {
                val payload =
                    dailyFortunePort.getIfAvailable()?.getMorningReport(setting.memberId) ?: DEFAULT_MORNING_PAYLOAD
                dispatch(setting.memberId, NotificationType.FORTUNE, payload)
            } catch (e: Exception) {
                log.error("아침 운 리포트 발송 실패: memberId=${setting.memberId}", e)
            }
        }
    }

    override fun dispatchLuckyActionReminder() {
        notificationSettingRepository.findAllLuckyActionReminderTargets().forEach { setting ->
            try {
                val payload =
                    luckyActionPort.getIfAvailable()?.getLuckyActionReminder(setting.memberId)
                        ?: DEFAULT_LUCKY_ACTION_PAYLOAD
                dispatch(setting.memberId, NotificationType.LUCKY_ACTION, payload)
            } catch (e: Exception) {
                log.error("행운 액션 리마인드 발송 실패: memberId=${setting.memberId}", e)
            }
        }
    }

    private fun dispatch(
        memberId: UUID,
        type: NotificationType,
        payload: NotificationPayload,
    ) = sendNotificationPort.send(
        SendNotificationCommand(
            memberId = memberId,
            type = type,
            title = payload.title,
            content = payload.content,
            deepLink = payload.deepLink,
        ),
    )
}

private val log = LoggerFactory.getLogger(ScheduledNotificationDispatchService::class.java)
private val DEFAULT_MORNING_PAYLOAD =
    NotificationPayload("오늘의 운이 도착했어요", "오늘 하루의 운세를 확인해 보세요.", "fortune/today")
private val DEFAULT_LUCKY_ACTION_PAYLOAD =
    NotificationPayload("오늘의 행운 액션", "오늘의 행운 액션을 완료했는지 확인해 보세요.", "lucky-action")
