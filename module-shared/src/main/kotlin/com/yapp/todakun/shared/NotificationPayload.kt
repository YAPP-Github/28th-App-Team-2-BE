package com.yapp.todakun.shared

/**
 * 스케줄 알림의 본문. 콘텐츠 확장 포트([DailyFortuneNotificationPort]/[LuckyActionNotificationPort])가 반환한다.
 */
data class NotificationPayload(
    val title: String,
    val content: String,
    val deepLink: String? = null,
)
