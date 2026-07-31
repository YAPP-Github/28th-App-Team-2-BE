package com.yapp.todakun.chat.port.outbound

import com.yapp.todakun.chat.ChatMessageRole

/** 토닥이 AI 프롬프트에 넣을 과거 대화 한 턴(멀티턴 맥락). 시간순(오래된 것부터)으로 나열된다. */
data class ChatHistoryTurn(
    val role: ChatMessageRole,
    val content: String,
)
