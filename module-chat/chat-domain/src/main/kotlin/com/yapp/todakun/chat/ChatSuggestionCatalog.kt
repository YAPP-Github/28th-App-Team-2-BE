package com.yapp.todakun.chat

import com.yapp.todakun.shared.FortuneCategory

/** 채팅 진입 화면(추천 질문 칩)의 결정적 정책. AI 호출 없이 고정 목록을 제공한다. */
object ChatSuggestionCatalog {
    const val GREETING = "오늘은 어떤 게 궁금해?"

    val suggestions: List<ChatSuggestion> =
        listOf(
            ChatSuggestion("🤝", "관계운", "요즘 인간관계에서 궁금한게 있어", FortuneCategory.RELATIONSHIP),
            ChatSuggestion("💌", "연애운", "요즘 그 사람과의 연애 흐름이 궁금해", FortuneCategory.LOVE),
            ChatSuggestion("💼", "성취운", "커리어 흐름이나, 목표에 대한 성과가 궁금해", FortuneCategory.ACHIEVEMENT),
            ChatSuggestion("💵", "금전운", "요즘 돈 관리, 소비 흐름을 알고 싶어", FortuneCategory.MONEY),
            ChatSuggestion("💪", "건강운", "건강 유지를 위해서 하면 좋을 것들이 뭐가 있을까?", FortuneCategory.HEALTH),
            ChatSuggestion("💬", "기타", "그외에 다른 것들이 궁금해", null),
        )
}
