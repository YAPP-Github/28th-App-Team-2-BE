package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import com.yapp.todakun.common.annotation.CommandService
import java.util.UUID

/** AI 생성 실패 시 어시스턴트 메시지를 실패 상태로 남기고, 실패한 시도만큼 하루 무료 채팅 쿼터를 되돌리는 짧은 트랜잭션. */
@CommandService
class FailChatTurnService(
    private val chatMessageRepository: ChatMessageRepository,
    private val chatQuotaPort: ChatQuotaPort,
) {
    fun fail(
        memberId: UUID,
        assistantMessageId: UUID,
    ) {
        val message =
            checkNotNull(chatMessageRepository.findById(assistantMessageId)) { "assistant message not found: $assistantMessageId" }
        chatMessageRepository.save(message.fail())
        chatQuotaPort.refund(memberId)
    }
}
