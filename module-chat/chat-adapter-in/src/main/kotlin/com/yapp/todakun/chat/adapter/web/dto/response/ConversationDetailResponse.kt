package com.yapp.todakun.chat.adapter.web.dto.response

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.port.inbound.ChatMessageResult
import com.yapp.todakun.chat.port.inbound.ConversationDetailResult
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ConversationDetailResponse(
    val id: UUID,
    val title: String,
    val messages: List<ChatMessageResponse>,
) {
    companion object {
        fun from(result: ConversationDetailResult): ConversationDetailResponse =
            ConversationDetailResponse(
                id = result.id,
                title = result.title,
                messages = result.messages.map(ChatMessageResponse::from),
            )
    }
}

data class ChatMessageResponse(
    val id: UUID,
    val role: String,
    val content: String,
    val status: String,
    val action: ChatActionResponse?,
    val createdAt: Instant?,
) {
    companion object {
        fun from(message: ChatMessageResult): ChatMessageResponse =
            ChatMessageResponse(
                id = message.id,
                role = message.role.name,
                content = message.content,
                status = message.status.name,
                action = message.action?.let(ChatActionResponse::from),
                createdAt = message.createdAt,
            )
    }
}

/** 답변 하단 액션 카드(예: "[계약・이사] 2026.7.25(토) · 내 캘린더에 추가하기"). */
data class ChatActionResponse(
    val type: String,
    val label: String,
    val category: String,
    val date: LocalDate,
) {
    companion object {
        fun from(action: ChatAction): ChatActionResponse =
            ChatActionResponse(
                type = action.type.name,
                label = action.label,
                category = action.category,
                date = action.date,
            )
    }
}
