package com.yapp.todakun.chat

import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * 대화 스레드. [title]은 첫 사용자 질문에서 파생되어 생성 시점에 고정되며(히스토리 목록 제목), 이후 바뀌지 않는다.
 * [unread]는 마지막 어시스턴트 답변을 아직 화면에서 보지 않았는지를 나타낸다(클라이언트가 SSE로 보고 있었으면 false로 완료).
 * [lastMessageAt]은 히스토리 정렬(최근 순) 기준이며 답변이 완료될 때마다 [touch]로 갱신한다.
 * (JPA `@LastModifiedDate`에 맡기면 [unread]가 실제로 값이 바뀌지 않는 흔한 경우 UPDATE 자체가 스킵되어 정렬 기준이 갱신되지 않으므로, 별도 필드로 명시적으로 관리한다.)
 */
data class ChatConversation(
    val id: UUID,
    val memberId: UUID,
    val title: String,
    val unread: Boolean,
    val lastMessageAt: Instant,
    val createdAt: Instant?,
) {
    /** 읽음/안읽음 상태를 갱신한 새 인스턴스를 반환한다(불변). 이미 같은 상태면 자신을 반환. */
    fun markUnread(unread: Boolean): ChatConversation = if (this.unread == unread) this else copy(unread = unread)

    /** 마지막 활동 시각을 갱신한다. 현재 시각은 호출부(application)가 주입한다(도메인은 시계에 의존하지 않는다). */
    fun touch(now: Instant): ChatConversation = copy(lastMessageAt = now)

    companion object {
        @ExperimentalUuidApi
        fun create(
            memberId: UUID,
            title: String,
            now: Instant,
        ): ChatConversation =
            ChatConversation(
                id = Uuid.generateV7().toJavaUuid(),
                memberId = memberId,
                title = title,
                unread = false,
                lastMessageAt = now,
                createdAt = null,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            memberId: UUID,
            title: String,
            unread: Boolean,
            lastMessageAt: Instant,
            createdAt: Instant?,
        ): ChatConversation = ChatConversation(id, memberId, title, unread, lastMessageAt, createdAt)
    }
}
