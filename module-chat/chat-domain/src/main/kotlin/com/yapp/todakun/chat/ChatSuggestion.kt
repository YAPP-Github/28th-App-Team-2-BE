package com.yapp.todakun.chat

import com.yapp.todakun.shared.FortuneCategory

/**
 * 채팅 진입 화면의 추천 질문 칩("관계운에 관하여 궁금해" 등). [category]가 null이면 카테고리 무관("그외에 다른 운이 궁금해") 칩이다.
 * [seedPrompt]는 칩 선택 시 사용자 메시지로 그대로 전송되는 시드 문장이다.
 */
data class ChatSuggestion(
    val emoji: String,
    val label: String,
    val seedPrompt: String,
    val category: FortuneCategory?,
)
