package com.yapp.todakun.notification.application

import com.yapp.todakun.notification.NotificationSetting
import com.yapp.todakun.notification.port.inbound.DispatchScheduledNotificationUseCase
import com.yapp.todakun.notification.port.inbound.PublishNoticeUseCase
import com.yapp.todakun.notification.port.outbound.DispatchLockPort
import com.yapp.todakun.notification.port.outbound.NotificationSettingRepository
import com.yapp.todakun.shared.DailyFortuneNotificationPort
import com.yapp.todakun.shared.GetMemberIdsPort
import com.yapp.todakun.shared.LuckyActionNotificationPort
import com.yapp.todakun.shared.NotificationPayload
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.UUID

private const val PAGE_SIZE = 100

// pg_advisory_lock 키(임의 상수). 다른 advisory lock 용도(BatchJdbcConfig 등)와 겹치지만 않으면 된다.
private const val MORNING_REPORT_LOCK_KEY = 8_412_037_601L
private const val LUCKY_ACTION_LOCK_KEY = 8_412_037_602L
private const val NOTICE_LOCK_KEY = 8_412_037_604L

/**
 * 알림 발송(아침 운 리포트/행운 액션 리마인드/공지) 대상 선별·콘텐츠 조달·발송 오케스트레이션.
 * 트랜잭션을 걸지 않는다 — 회원별 [SendNotificationPort.send] 호출이 각자 독립 트랜잭션(SendNotificationService)을
 * 갖게 해, 회원 수에 비례해 트랜잭션이 길어지는 문제를 없앤다(#41과 동일 원칙).
 * 콘텐츠 확장 포트([dailyFortunePort]/[luckyActionPort])는 옵셔널 주입 — 구현 빈이 없거나 콘텐츠가 없으면(null)
 * 해당 회원 발송을 스킵한다.
 * 대상 회원별 발송은 예외를 격리해 한 명의 실패가 나머지 회원의 발송을 중단시키지 않는다.
 * OutOfMemoryError 등 복구 불가능한 [Error]는 격리 대상이 아니므로 [Exception]만 명시적으로 잡아 전파시킨다.
 * Blue/Green 배포 전환 구간의 중복 실행은 [dispatchLockPort]로 인스턴스 간 직렬화한다. 공지([publish])는
 * 관리자 API로도 호출 가능해져(더 이상 운영 스크립트 단독 진입점이 아님) 동시 중복 실행 방지를 위해 락을 건다 —
 * 다만 서로 다른 시점의 반복 호출까지 막는 완전한 멱등성은 별도 과제다.
 */
@Service
class NotificationDispatchService(
    private val notificationSettingRepository: NotificationSettingRepository,
    private val sendNotificationPort: SendNotificationPort,
    private val getMemberIdsPort: GetMemberIdsPort,
    private val dispatchLockPort: DispatchLockPort,
    private val dailyFortunePort: ObjectProvider<DailyFortuneNotificationPort>,
    private val luckyActionPort: ObjectProvider<LuckyActionNotificationPort>,
) : DispatchScheduledNotificationUseCase, PublishNoticeUseCase {
    override fun dispatchMorningReport(slot: LocalTime) {
        dispatchLockPort.tryRun(MORNING_REPORT_LOCK_KEY) {
            dispatchInChunks(
                fetchChunk = { afterId -> notificationSettingRepository.findMorningReportTargets(slot, afterId, PAGE_SIZE) },
                idOf = NotificationSetting::id,
                memberIdOf = NotificationSetting::memberId,
                type = NotificationType.FORTUNE,
                resolvePayload = { dailyFortunePort.getIfAvailable()?.getMorningReport(it.memberId) },
            )
        } ?: log.info("다른 인스턴스가 이미 아침 운 리포트를 처리 중이라 스킵합니다: slot=$slot")
    }

    override fun dispatchLuckyActionReminder() {
        dispatchLockPort.tryRun(LUCKY_ACTION_LOCK_KEY) {
            dispatchInChunks(
                fetchChunk = { afterId -> notificationSettingRepository.findLuckyActionReminderTargets(afterId, PAGE_SIZE) },
                idOf = NotificationSetting::id,
                memberIdOf = NotificationSetting::memberId,
                type = NotificationType.LUCKY_ACTION,
                resolvePayload = { luckyActionPort.getIfAvailable()?.getLuckyActionReminder(it.memberId) },
            )
        } ?: log.info("다른 인스턴스가 이미 행운 액션 리마인드를 처리 중이라 스킵합니다.")
    }

    override fun publish(
        title: String,
        content: String,
        deepLink: String?,
    ) {
        dispatchLockPort.tryRun(NOTICE_LOCK_KEY) {
            dispatchInChunks(
                fetchChunk = { afterId -> getMemberIdsPort.getMemberIds(afterId, PAGE_SIZE) },
                idOf = { it },
                memberIdOf = { it },
                type = NotificationType.NOTICE,
                resolvePayload = { NotificationPayload(title, content, deepLink) },
            )
        } ?: log.info("다른 인스턴스가 이미 공지를 발송 중이라 스킵합니다.")
    }

    private fun <T> dispatchInChunks(
        fetchChunk: (afterId: UUID?) -> List<T>,
        idOf: (T) -> UUID,
        memberIdOf: (T) -> UUID,
        type: NotificationType,
        resolvePayload: (T) -> NotificationPayload?,
    ) {
        var afterId: UUID? = null
        while (true) {
            val chunk = fetchChunk(afterId)
            if (chunk.isEmpty()) return

            chunk.forEach { item ->
                try {
                    val payload = resolvePayload(item) ?: return@forEach
                    sendNotificationPort.send(
                        SendNotificationCommand(
                            memberId = memberIdOf(item),
                            type = type,
                            title = payload.title,
                            content = payload.content,
                            deepLink = payload.deepLink,
                        ),
                    )
                } catch (e: Exception) {
                    log.error("알림 발송 실패: memberId=${memberIdOf(item)}, type=$type", e)
                }
            }
            afterId = idOf(chunk.last())
        }
    }
}

private val log = LoggerFactory.getLogger(NotificationDispatchService::class.java)
