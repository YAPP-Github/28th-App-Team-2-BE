package com.yapp.todakun.chat

import com.yapp.todakun.shared.FortuneCategory

/** 채팅 진입 화면(추천 질문 칩)의 결정적 정책. AI 호출 없이 고정 목록을 제공한다. */
object ChatSuggestionCatalog {
    const val GREETING = "오늘은 어떤게 궁금해?"

    val suggestions: List<ChatSuggestion> =
        listOf(
            ChatSuggestion("🤝", "관계운에 관하여 궁금해", "요즘 관계운이 궁금해.", FortuneCategory.RELATIONSHIP),
            ChatSuggestion("💌", "연애운에 관하여 궁금해", "요즘 연애운이 궁금해.", FortuneCategory.LOVE),
            ChatSuggestion("💼", "성취운에 관하여 궁금해", "요즘 성취운이 궁금해.", FortuneCategory.ACHIEVEMENT),
            ChatSuggestion("🧧", "금전운에 관하여 궁금해", "요즘 금전운이 궁금해.", FortuneCategory.MONEY),
            ChatSuggestion("💪", "건강운에 관하여 궁금해", "요즘 건강운이 궁금해.", FortuneCategory.HEALTH),
            ChatSuggestion("💬", "그외에 다른 운이 궁금해", "요즘 궁금한 게 있어.", null),
        )
}
