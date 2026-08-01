package com.yapp.todakun.chat.adapter.web.dto.response

import com.yapp.todakun.chat.ChatSuggestion
import com.yapp.todakun.chat.port.inbound.ChatEntryResult

data class ChatEntryResponse(
    val greeting: String,
    val suggestions: List<ChatSuggestionResponse>,
    val quota: ChatQuotaResponse,
) {
    companion object {
        fun from(result: ChatEntryResult): ChatEntryResponse =
            ChatEntryResponse(
                greeting = result.greeting,
                suggestions = result.suggestions.map(ChatSuggestionResponse::from),
                quota = ChatQuotaResponse(result.quotaUsed, result.quotaLimit),
            )
    }
}

/** 추천 질문 칩. [category]는 관련 운세 카테고리 코드이며, 카테고리 무관 칩("그외에 다른 운이 궁금해")이면 null이다. */
data class ChatSuggestionResponse(
    val emoji: String,
    val label: String,
    val seedPrompt: String,
    val category: String?,
) {
    companion object {
        fun from(suggestion: ChatSuggestion): ChatSuggestionResponse =
            ChatSuggestionResponse(
                emoji = suggestion.emoji,
                label = suggestion.label,
                seedPrompt = suggestion.seedPrompt,
                category = suggestion.category?.name,
            )
    }
}

data class ChatQuotaResponse(
    val used: Int,
    val limit: Int,
)
