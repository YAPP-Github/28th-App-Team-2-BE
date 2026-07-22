package com.yapp.todakun.notification

import com.yapp.todakun.shared.NotificationType
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 회원별 인앱 알림함 레코드. 계획 스키마(docs/todakun.sql)의 notification + member_notification을
 * 회원별 단일 레코드로 병합한다 — FORTUNE/AI_COMPLETE는 회원마다 본문이 달라 공용 콘텐츠 정규화가 이점이 없고,
 * NOTICE 공지만 회원 수만큼 fan-out insert로 처리하면 되기 때문.
 * [createdAt]은 JPA 감사(auditing)가 채우므로 생성 시점에는 null이다.
 */
data class Notification(
    val id: UUID,
    val memberId: UUID,
    val type: NotificationType,
    val title: String,
    val content: String,
    val deepLink: String?,
    val isRead: Boolean,
    val createdAt: Instant?,
) {
    /** 읽음 처리된 새 인스턴스를 반환한다(불변). 이미 읽음이면 자신을 반환. */
    fun markAsRead(): Notification = if (isRead) this else copy(isRead = true)

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun create(
            memberId: UUID,
            type: NotificationType,
            title: String,
            content: String,
            deepLink: String? = null,
        ): Notification =
            Notification(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                type = type,
                title = title,
                content = content,
                deepLink = deepLink,
                isRead = false,
                createdAt = null,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            type: NotificationType,
            title: String,
            content: String,
            deepLink: String?,
            isRead: Boolean,
            createdAt: Instant?,
        ): Notification = Notification(id, memberId, type, title, content, deepLink, isRead, createdAt)
    }
}
