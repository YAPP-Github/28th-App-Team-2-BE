package com.yapp.todakun.chat.port.inbound

import java.util.UUID

/**
 * 토닥이 답변 스트리밍 유스케이스. [conversationId]가 null이면 새 대화("새 채팅")를 시작하고, 있으면 이어간다.
 * SSE 연결이 끊겨도 답변 생성·저장·알림 발송은 [listener]와 무관하게 끝까지 진행된다(동기 SSE + 생성 스레드 분리).
 */
interface StreamChatAnswerUseCase {
    fun stream(
        command: SendChatMessageCommand,
        listener: ChatStreamListener,
    )
}

data class SendChatMessageCommand(
    val memberId: UUID,
    val conversationId: UUID?,
    val content: String,
)
