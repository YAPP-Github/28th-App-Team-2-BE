package com.yapp.todakun.chat.port.outbound

/** [ChatAiPort]에 전달되는 프롬프트 구성 요소 전체(명식·프로필·과거 대화·이번 질문). */
data class ChatPromptContext(
    val saju: ChatSajuContext,
    val profile: ChatProfileContext,
    val history: List<ChatHistoryTurn>,
    val question: String,
)
