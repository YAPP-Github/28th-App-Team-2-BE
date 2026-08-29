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
 * [errorCode]는 실패 원인 진단용(FCM MessagingErrorCode 등)이며, 영속되지 않고 로그·메트릭에만 쓰인다.
 */
data class PushResult(
    val token: String,
    val success: Boolean,
    val tokenExpired: Boolean = false,
    val errorCode: String? = null,
)
