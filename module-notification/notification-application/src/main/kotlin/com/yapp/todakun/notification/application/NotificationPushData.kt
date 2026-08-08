package com.yapp.todakun.notification.application

import com.yapp.todakun.shared.NotificationType
import java.util.UUID

/**
 * FCM data payload 빌더. 최초 발송([SendNotificationService])과 재시도([RetryFailedNotificationsService])가
 * 클라이언트 라우팅에 필요한 동일한 데이터를 구성하도록 공유한다.
 */
internal fun buildPushData(
    type: NotificationType,
    notificationId: UUID,
    deepLink: String?,
): Map<String, String> =
    buildMap {
        put("type", type.name)
        put("notificationId", notificationId.toString())
        deepLink?.let { put("deepLink", it) }
    }
