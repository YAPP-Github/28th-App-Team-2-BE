package com.yapp.todakun.shared

import java.util.UUID

/**
 * 아침 운 리포트(FORTUNE) 본문 조달 확장점. "오늘의 운세" 도메인이 생기면 이 포트를 구현하는 빈을 추가한다.
 * 구현 빈이 없으면 notification 측 기본 문구가 사용된다(발송 로직은 그대로 유지).
 */
interface DailyFortuneNotificationPort {
    fun getMorningReport(memberId: UUID): NotificationPayload?
}
