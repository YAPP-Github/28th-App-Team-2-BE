package com.yapp.todakun.chat

import com.yapp.todakun.chat.exception.ChatContentTooLongException
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private const val USER_CONTENT_MAX_LENGTH = 500

/**
 * 대화 스레드에 속한 메시지 한 건. USER 메시지는 생성 즉시 [ChatMessageStatus.COMPLETED]이고,
 * ASSISTANT 메시지는 [createAssistantPlaceholder]로 빈 본문·[ChatMessageStatus.GENERATING] 상태로 먼저 저장한 뒤
 * 스트리밍이 끝나면 [complete]로 확정한다(중간에 연결이 끊겨도 생성은 이어지므로 완성본을 놓치지 않기 위함).
 */
data class ChatMessage(
    val id: UUID,
    val conversationId: UUID,
    val role: ChatMessageRole,
    val content: String,
    val status: ChatMessageStatus,
    val action: ChatAction?,
    val createdAt: Instant?,
) {
    /** 스트리밍이 끝난 어시스턴트 메시지를 완성본으로 확정한다. */
    fun complete(
        content: String,
        action: ChatAction?,
    ): ChatMessage = copy(content = content, status = ChatMessageStatus.COMPLETED, action = action)

    /** AI 생성 실패 시 메시지를 실패 상태로 남긴다(빈 본문 유지). */
    fun fail(): ChatMessage = copy(status = ChatMessageStatus.FAILED)

    companion object {
        @ExperimentalUuidApi
        fun createUser(
            conversationId: UUID,
            content: String,
        ): ChatMessage {
            validateContentLength(content)

            return ChatMessage(
                id = Uuid.generateV7().toJavaUuid(),
                conversationId = conversationId,
                role = ChatMessageRole.USER,
                content = content,
                status = ChatMessageStatus.COMPLETED,
                action = null,
                createdAt = null,
            )
        }

        @ExperimentalUuidApi
        fun createAssistantPlaceholder(conversationId: UUID): ChatMessage =
            ChatMessage(
                id = Uuid.generateV7().toJavaUuid(),
                conversationId = conversationId,
                role = ChatMessageRole.ASSISTANT,
                content = "",
                status = ChatMessageStatus.GENERATING,
                action = null,
                createdAt = null,
            )

        @JvmStatic
        fun reconstitute(
            id: UUID,
            conversationId: UUID,
            role: ChatMessageRole,
            content: String,
            status: ChatMessageStatus,
            action: ChatAction?,
            createdAt: Instant?,
        ): ChatMessage = ChatMessage(id, conversationId, role, content, status, action, createdAt)

        private fun validateContentLength(content: String) {
            if (content.length > USER_CONTENT_MAX_LENGTH) {
                throw ChatContentTooLongException()
            }
        }
    }
}
