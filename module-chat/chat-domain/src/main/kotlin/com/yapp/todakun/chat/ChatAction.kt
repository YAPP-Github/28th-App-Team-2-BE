package com.yapp.todakun.chat

import java.time.LocalDate

/**
 * 답변 본문 아래 표시되는 액션 카드(예: "[계약・이사] 2026.7.25(토) · 내 캘린더에 추가하기").
 * AI가 답변 완성 후 별도 호출([com.yapp.todakun.chat.port.outbound.ChatAiPort.extractAction])로 추출하며, 없으면 null이다.
 */
data class ChatAction(
    val type: ChatActionType,
    val label: String,
    val category: String,
    val date: LocalDate,
)
