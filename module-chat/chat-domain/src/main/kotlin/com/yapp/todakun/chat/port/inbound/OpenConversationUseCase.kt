package com.yapp.todakun.chat.port.inbound

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatConversation
import com.yapp.todakun.chat.ChatMessage
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.ChatMessageStatus
import java.time.Instant
import java.util.UUID

/** 대화 상세(메시지 목록) 조회 유스케이스. 조회 시 해당 대화를 읽음 처리한다(안읽음 배지 해제). */
interface OpenConversationUseCase {
    fun open(
        memberId: UUID,
        conversationId: UUID,
    ): ConversationDetailResult
}

data class ChatMessageResult(
    val id: UUID,
    val role: ChatMessageRole,
    val content: String,
    val status: ChatMessageStatus,
    val action: ChatAction?,
    val createdAt: Instant?,
) {
    companion object {
        fun from(message: ChatMessage): ChatMessageResult =
            ChatMessageResult(
                id = message.id,
                role = message.role,
                content = message.content,
                status = message.status,
                action = message.action,
                createdAt = message.createdAt,
            )
    }
}

data class ConversationDetailResult(
    val id: UUID,
    val title: String,
    val messages: List<ChatMessageResult>,
) {
    companion object {
        fun from(
            conversation: ChatConversation,
            messages: List<ChatMessage>,
        ): ConversationDetailResult =
            ConversationDetailResult(
                id = conversation.id,
                title = conversation.title,
                messages = messages.map(ChatMessageResult::from),
            )
    }
}
