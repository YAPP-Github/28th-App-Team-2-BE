package com.yapp.todakun.notification.port.outbound

import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.PushResult

/**
 * FCM 발송 아웃바운드 포트. adapter.fcm의 어댑터가 구현한다.
 * 포트는 도메인 타입만 다루며, Message 빌드·전송·에러코드 해석은 어댑터 내부에서 끝난다.
 */
interface PushNotificationPort {
    fun send(notification: PushNotification): PushResult

    fun sendAll(notifications: List<PushNotification>): List<PushResult>
}
