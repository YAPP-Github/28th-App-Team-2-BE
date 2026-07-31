package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.port.inbound.DeleteConversationUseCase
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.common.annotation.CommandService
import java.util.UUID

@CommandService
class DeleteConversationService(
    private val chatConversationRepository: ChatConversationRepository,
    private val chatMessageRepository: ChatMessageRepository,
) : DeleteConversationUseCase {
    override fun delete(
        memberId: UUID,
        conversationId: UUID,
    ) {
        val conversation = chatConversationRepository.findById(conversationId) ?: throw ChatConversationNotFoundException()
        if (conversation.memberId != memberId) throw ChatConversationForbiddenException()

        chatMessageRepository.deleteAllByConversationId(conversationId)
        chatConversationRepository.deleteById(conversationId)
    }
}
