package com.yapp.todakun.chat.adapter.web.dto.response

import com.yapp.todakun.chat.port.inbound.ConversationListResult
import com.yapp.todakun.chat.port.inbound.ConversationSummaryResult
import java.time.Instant
import java.util.UUID

data class ConversationListResponse(
    val conversations: List<ConversationSummaryResponse>,
) {
    companion object {
        fun from(result: ConversationListResult): ConversationListResponse =
            ConversationListResponse(result.conversations.map(ConversationSummaryResponse::from))
    }
}

data class ConversationSummaryResponse(
    val id: UUID,
    val title: String,
    val lastMessageAt: Instant?,
    val unread: Boolean,
) {
    companion object {
        fun from(result: ConversationSummaryResult): ConversationSummaryResponse =
            ConversationSummaryResponse(
                id = result.id,
                title = result.title,
                lastMessageAt = result.lastMessageAt,
                unread = result.unread,
            )
    }
}
