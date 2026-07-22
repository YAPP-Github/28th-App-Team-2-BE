package com.yapp.todakun.shared

import java.util.UUID

/**
 * 행운 액션 리마인드(LUCKY_ACTION) 본문 조달 확장점. "행운 액션" 도메인이 생기면 이 포트를 구현하는 빈을 추가한다.
 * 구현 빈이 없으면 notification 측 기본 문구가 사용된다.
 */
interface LuckyActionNotificationPort {
    fun getLuckyActionReminder(memberId: UUID): NotificationPayload?
}
