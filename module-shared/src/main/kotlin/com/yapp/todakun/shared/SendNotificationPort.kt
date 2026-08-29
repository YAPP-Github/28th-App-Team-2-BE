package com.yapp.todakun.shared

import java.util.UUID

/**
 * 임의 도메인이 회원에게 알림을 발급하는 크로스도메인 진입점.
 * notification-application의 SendNotificationService가 구현한다.
 * 생산자 도메인은 :shared에만 의존해 이 포트를 호출하므로, MSA 분리 시 구현체만 HTTP 어댑터로 교체된다.
 */
interface SendNotificationPort {
    fun send(command: SendNotificationCommand)
}

/**
 * [push]가 true여도 회원의 알림 설정 토글이 꺼져 있으면 인앱 알림함에만 저장되고 푸시는 생략된다.
 */
data class SendNotificationCommand(
    val memberId: UUID,
    val type: NotificationType,
    val title: String,
    val content: String,
    val deepLink: String? = null,
    val push: Boolean = true,
)
