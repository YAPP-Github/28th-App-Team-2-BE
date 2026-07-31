package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.common.annotation.CommandService
import java.time.Instant
import java.util.UUID

/** 스트리밍이 끝난 어시스턴트 메시지를 확정하는 짧은 트랜잭션. AI 호출 자체는 이 서비스 밖(트랜잭션 밖)에서 이미 끝난 상태로 들어온다. */
@CommandService
class CompleteChatTurnService(
    private val chatConversationRepository: ChatConversationRepository,
    private val chatMessageRepository: ChatMessageRepository,
) {
    fun complete(
        conversationId: UUID,
        assistantMessageId: UUID,
        answer: String,
        action: ChatAction?,
        unread: Boolean,
    ) {
        // 두 메시지·대화 모두 바로 이전 단계(PrepareChatTurnService)에서 우리가 직접 생성한 것이라 항상 존재해야 하는 내부 불변식이다.
        val message =
            checkNotNull(chatMessageRepository.findById(assistantMessageId)) { "assistant message not found: $assistantMessageId" }
        chatMessageRepository.save(message.complete(answer, action))

        val conversation = checkNotNull(chatConversationRepository.findById(conversationId)) { "conversation not found: $conversationId" }
        chatConversationRepository.save(conversation.touch(Instant.now()).markUnread(unread))
    }
}
