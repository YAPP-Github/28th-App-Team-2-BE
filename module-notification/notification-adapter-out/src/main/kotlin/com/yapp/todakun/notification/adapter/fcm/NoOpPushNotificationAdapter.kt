package com.yapp.todakun.notification.adapter.fcm

import com.yapp.todakun.notification.PushNotification
import com.yapp.todakun.notification.PushResult
import com.yapp.todakun.notification.port.outbound.PushNotificationPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * fcm.enabled=false(로컬 기본)일 때의 무발송 구현. 인앱 알림함/설정 등 나머지 기능은 그대로 동작시키고
 * FCM 발송만 no-op 처리해, Firebase 자격증명 없이도 앱이 기동되도록 한다(PushNotificationPort 빈은 항상 1개 존재).
 */
@Component
@ConditionalOnProperty(prefix = "fcm", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class NoOpPushNotificationAdapter : PushNotificationPort {
    override fun send(notification: PushNotification): PushResult = PushResult(token = notification.token, success = true)

    override fun sendAll(notifications: List<PushNotification>): List<PushResult> =
        notifications.map { PushResult(token = it.token, success = true) }
}
