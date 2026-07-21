package com.yapp.todakun.notification

/**
 * FCM 발송 요청 값 타입(순수 도메인). Firebase 타입은 adapter.fcm 안에서만 다룬다.
 */
data class PushNotification(
    val token: String,
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
)

/**
 * FCM 발송 결과. [tokenExpired]가 true면(UNREGISTERED/INVALID) application 계층이 토큰을 정리한다.
 */
data class PushResult(
    val token: String,
    val success: Boolean,
    val tokenExpired: Boolean = false,
)
