package com.yapp.todakun.notification.adapter.scheduler

import com.yapp.todakun.notification.port.inbound.DispatchScheduledNotificationUseCase
import com.yapp.todakun.notification.port.inbound.RetryFailedNotificationsUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalTime
import java.time.ZoneId

/**
 * 스케줄 알림의 시각 트리거(driving adapter). 대상 선별·발송은 application 유스케이스가 담당한다.
 * 모든 스케줄은 Asia/Seoul 기준.
 */
@Component
class NotificationScheduler(
    private val dispatchScheduledNotificationUseCase: DispatchScheduledNotificationUseCase,
    private val retryFailedNotificationsUseCase: RetryFailedNotificationsUseCase,
) {
    // 아침 운 리포트: 매 정시·30분에 깨어, 그 반시각을 받을 시간으로 지정한 회원에게 발송.
    @Scheduled(cron = "0 0,30 * * * *", zone = SEOUL)
    fun sendMorningReports() {
        val now = LocalTime.now(ZoneId.of(SEOUL))
        val slot = LocalTime.of(now.hour, if (now.minute < 30) 0 else 30)
        dispatchScheduledNotificationUseCase.dispatchMorningReport(slot)
    }

    // 행운 액션 리마인드: 매일 20:00 고정.
    @Scheduled(cron = "0 0 20 * * *", zone = SEOUL)
    fun sendLuckyActionReminders() {
        dispatchScheduledNotificationUseCase.dispatchLuckyActionReminder()
    }

    // 발송 실패 재시도: 매분 확인. 실제 대상은 nextRetryAt이 지난 건만 골라내므로 매분 트리거해도 부담이 없다.
    @Scheduled(cron = "0 * * * * *", zone = SEOUL)
    fun retryFailedNotifications() {
        retryFailedNotificationsUseCase.retryDue()
    }

    companion object {
        private const val SEOUL = "Asia/Seoul"
    }
}
