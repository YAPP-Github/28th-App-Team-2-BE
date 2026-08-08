package com.yapp.todakun.notification

import com.yapp.todakun.shared.NotificationType
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * FCM 발송이 일시적으로 실패(무효 토큰이 아닌 네트워크·FCM 오류)한 건의 재시도 대기열.
 * `notification.md` 7절 재시도 정책([com.yapp.todakun.notification.policy.NotificationRetryPolicy] 참고)을 뒷받침한다.
 * [notificationId]는 이미 저장된 인앱 알림함 레코드를 가리킨다 — 재시도 시 동일한 딥링크 데이터로 푸시를 재구성하기 위함이다.
 */
data class NotificationDeliveryFailure(
    val id: UUID,
    val memberId: UUID,
    val notificationId: UUID,
    val type: NotificationType,
    val title: String,
    val content: String,
    val deepLink: String?,
    val attemptCount: Int,
    val nextRetryAt: Instant,
) {
    /** 재시도가 다시 실패했을 때 다음 재시도 정보로 갱신한 새 인스턴스를 반환한다(불변). */
    fun scheduleNextRetry(nextRetryAt: Instant): NotificationDeliveryFailure =
        copy(attemptCount = attemptCount + 1, nextRetryAt = nextRetryAt)

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            notificationId: UUID,
            type: NotificationType,
            title: String,
            content: String,
            deepLink: String?,
            nextRetryAt: Instant,
        ): NotificationDeliveryFailure =
            NotificationDeliveryFailure(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                notificationId = notificationId,
                type = type,
                title = title,
                content = content,
                deepLink = deepLink,
                attemptCount = 0,
                nextRetryAt = nextRetryAt,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            notificationId: UUID,
            type: NotificationType,
            title: String,
            content: String,
            deepLink: String?,
            attemptCount: Int,
            nextRetryAt: Instant,
        ): NotificationDeliveryFailure =
            NotificationDeliveryFailure(id, memberId, notificationId, type, title, content, deepLink, attemptCount, nextRetryAt)
    }
}
