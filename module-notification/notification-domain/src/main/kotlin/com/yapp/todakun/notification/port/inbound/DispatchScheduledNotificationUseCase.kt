package com.yapp.todakun.notification.port.inbound

import java.time.LocalTime

/**
 * 스케줄러(driving adapter)가 시각 트리거로 호출하는 스케줄 알림 발송 유스케이스.
 * 대상 회원 선별·콘텐츠 조달·발송은 application이 담당한다.
 */
interface DispatchScheduledNotificationUseCase {
    /** 받을 시간이 [slot]인(30분 단위) 아침 운 리포트 대상에게 발송. */
    fun dispatchMorningReport(slot: LocalTime)

    /** 행운 액션 리마인드 대상에게 발송(매일 20:00 고정). */
    fun dispatchLuckyActionReminder()
}
