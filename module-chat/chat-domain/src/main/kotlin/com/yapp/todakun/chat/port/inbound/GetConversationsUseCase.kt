package com.yapp.todakun.chat.port.inbound

import com.yapp.todakun.chat.ChatConversation
import java.time.Instant
import java.util.UUID

/** 회원의 대화 히스토리 목록 조회 유스케이스(최근 활동 순). */
interface GetConversationsUseCase {
    fun getConversations(memberId: UUID): ConversationListResult
}

data class ConversationSummaryResult(
    val id: UUID,
    val title: String,
    val lastMessageAt: Instant?,
    val unread: Boolean,
) {
    companion object {
        fun from(conversation: ChatConversation): ConversationSummaryResult =
            ConversationSummaryResult(
                id = conversation.id,
                title = conversation.title,
                lastMessageAt = conversation.lastMessageAt,
                unread = conversation.unread,
            )
    }
}

data class ConversationListResult(
    val conversations: List<ConversationSummaryResult>,
)
