package com.yapp.todakun.chat.port.inbound

import com.yapp.todakun.chat.ChatAction
import java.util.UUID

/**
 * [StreamChatAnswerUseCase]가 생성 진행 상황을 콜백으로 알리는 대상. adapter-in의 SseEmitter 래퍼가 구현한다.
 * [isClientConnected]는 완료 시 푸시 발송 여부/안읽음 처리 여부를 결정하는 데 쓰인다
 * (연결을 유지 중이면 화면으로 보고 있다고 간주해 푸시를 생략하고 읽음 처리한다).
 */
interface ChatStreamListener {
    fun onStart(started: ChatTurnStarted)

    fun onDelta(text: String)

    fun onAction(action: ChatAction)

    fun onDone(assistantMessageId: UUID)

    fun onError(error: Throwable)

    fun isClientConnected(): Boolean
}

data class ChatTurnStarted(
    val conversationId: UUID,
    val userMessageId: UUID,
    val assistantMessageId: UUID,
    val quotaUsed: Int,
    val quotaLimit: Int,
)
