package com.yapp.todakun.chat.adapter.web.sse

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.port.inbound.ChatTurnStarted
import java.time.LocalDate
import java.util.UUID

/** `event: start` — SSE 연결 직후 대화/메시지 ID와 남은 쿼터를 전달한다. */
data class ChatStartEvent(
    val conversationId: UUID,
    val userMessageId: UUID,
    val assistantMessageId: UUID,
    val quotaUsed: Int,
    val quotaLimit: Int,
) {
    companion object {
        fun from(started: ChatTurnStarted): ChatStartEvent =
            ChatStartEvent(
                conversationId = started.conversationId,
                userMessageId = started.userMessageId,
                assistantMessageId = started.assistantMessageId,
                quotaUsed = started.quotaUsed,
                quotaLimit = started.quotaLimit,
            )
    }
}

/** `event: delta` — 답변 토큰 조각. */
data class ChatDeltaEvent(
    val text: String,
)

/** `event: action` — 답변 하단 액션 카드(있을 때만 전송). */
data class ChatActionEvent(
    val type: String,
    val label: String,
    val category: String,
    val date: LocalDate,
) {
    companion object {
        fun from(action: ChatAction): ChatActionEvent =
            ChatActionEvent(type = action.type.name, label = action.label, category = action.category, date = action.date)
    }
}

/** `event: done` — 스트리밍 정상 종료. */
data class ChatDoneEvent(
    val assistantMessageId: UUID,
)

/** `event: error` — 준비 단계 실패(쿼터 소진·대화 없음/권한 등) 또는 생성 실패 시 전송. */
data class ChatErrorEvent(
    val code: String,
    val message: String,
)
