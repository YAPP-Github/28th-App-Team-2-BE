package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.port.outbound.ChatPromptContext
import com.yapp.todakun.chat.port.outbound.ChatQuotaStatus
import java.util.UUID

/** [PrepareChatTurnService]의 산출물. 오케스트레이터([StreamChatAnswerService])가 스트리밍·완료·알림 단계에 필요한 값을 한 번에 전달받는다. */
data class PreparedChatTurn(
    val memberId: UUID,
    val conversationId: UUID,
    val conversationTitle: String,
    val userMessageId: UUID,
    val assistantMessageId: UUID,
    val quota: ChatQuotaStatus,
    val promptContext: ChatPromptContext,
)
