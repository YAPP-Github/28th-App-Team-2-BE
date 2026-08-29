package com.yapp.todakun.chat.port.outbound

import java.time.LocalDate

/**
 * [ChatAiPort]에 전달되는 프롬프트 구성 요소 전체(오늘 날짜·명식·프로필·과거 대화·이번 질문).
 *
 * [today]는 KST 기준 "오늘"이다. LLM은 상태가 없어 현재 시각을 모르고, 알려주지 않으면 학습 데이터 분포에 이끌려
 * 과거 연도를 "올해"로 답하므로(예: 2024년), 프롬프트에 반드시 기준일을 명시한다.
 */
data class ChatPromptContext(
    val today: LocalDate,
    val saju: ChatSajuContext,
    val profile: ChatProfileContext,
    val history: List<ChatHistoryTurn>,
    val question: String,
)
