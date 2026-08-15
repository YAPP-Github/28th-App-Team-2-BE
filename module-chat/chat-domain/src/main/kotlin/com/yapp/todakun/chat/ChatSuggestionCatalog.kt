package com.yapp.todakun.chat

import com.yapp.todakun.shared.FortuneCategory

/** 채팅 진입 화면(추천 질문 칩)의 결정적 정책. AI 호출 없이 고정 목록을 제공한다. */
object ChatSuggestionCatalog {
    const val GREETING = "오늘은 어떤 게 궁금해?"

    val suggestions: List<ChatSuggestion> =
        listOf(
            ChatSuggestion(
                "🤝",
                "요즘 인간관계에서 궁금한게 있어",
                "인간관계에서 궁금한 걸 물어봐! 가족, 친구, 동료와의 관계 등 뭐든 괜찮아.",
                FortuneCategory.RELATIONSHIP,
            ),
            ChatSuggestion(
                "💌",
                "요즘 그 사람과의 연애 흐름이 궁금해",
                "그 사람과의 연애, 궁금한 걸 물어봐! 짝사랑, 재회, 갈등 등 뭐든 괜찮아.",
                FortuneCategory.LOVE,
            ),
            ChatSuggestion(
                "💼",
                "커리어 흐름이나, 목표에 대한 성과가 궁금해",
                "커리어에서 궁금한 걸 물어봐! 이직, 승진, 목표 달성 등 뭐든 괜찮아.",
                FortuneCategory.ACHIEVEMENT,
            ),
            ChatSuggestion(
                "💵",
                "요즘 돈 관리, 소비 흐름을 알고 싶어",
                "돈 관리에서 궁금한 걸 물어봐! 저축, 지출, 투자 등 뭐든 괜찮아.",
                FortuneCategory.MONEY,
            ),
            ChatSuggestion(
                "💪",
                "요즘 컨디션, 건강운으로 점검해줘",
                "요즘 컨디션에서 궁금한 걸 물어봐! 스트레스, 생활 습관 등 뭐든 괜찮아.",
                FortuneCategory.HEALTH,
            ),
            ChatSuggestion(
                "💬",
                "그외에 다른 운이 궁금해",
                "그 외에 궁금한 게 있으면 물어봐! 사소한 것부터 마음속 얘기까지 다 괜찮아.",
                null,
            ),
        )
}
