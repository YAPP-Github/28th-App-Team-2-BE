package com.yapp.todakun.chat.port.inbound

import com.yapp.todakun.chat.ChatSuggestion
import com.yapp.todakun.chat.ChatSuggestionCatalog
import java.util.UUID

/** 채팅 진입 화면(추천 질문 칩 + 오늘의 무료 채팅 사용량) 조회 유스케이스. */
interface GetChatEntryUseCase {
    fun getEntry(memberId: UUID): ChatEntryResult
}

data class ChatEntryResult(
    val greeting: String,
    val suggestions: List<ChatSuggestion>,
    val quotaUsed: Int,
    val quotaLimit: Int,
) {
    companion object {
        fun of(
            quotaUsed: Int,
            quotaLimit: Int,
        ): ChatEntryResult =
            ChatEntryResult(
                greeting = ChatSuggestionCatalog.GREETING,
                suggestions = ChatSuggestionCatalog.suggestions,
                quotaUsed = quotaUsed,
                quotaLimit = quotaLimit,
            )
    }
}
