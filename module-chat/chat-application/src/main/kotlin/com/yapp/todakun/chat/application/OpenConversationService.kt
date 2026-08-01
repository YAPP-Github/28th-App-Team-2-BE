package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.port.inbound.ConversationDetailResult
import com.yapp.todakun.chat.port.inbound.OpenConversationUseCase
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.common.annotation.CommandService
import java.util.UUID

/** 대화 상세를 조회하고, 조회와 함께 읽음 처리한다(안읽음 배지 해제) — 조회이지만 부수효과(쓰기)가 있어 [CommandService]다. */
@CommandService
class OpenConversationService(
    private val chatConversationRepository: ChatConversationRepository,
    private val chatMessageRepository: ChatMessageRepository,
) : OpenConversationUseCase {
    override fun open(
        memberId: UUID,
        conversationId: UUID,
    ): ConversationDetailResult {
        val conversation = chatConversationRepository.findById(conversationId) ?: throw ChatConversationNotFoundException()
        if (conversation.memberId != memberId) throw ChatConversationForbiddenException()

        val read = conversation.markUnread(false)
        if (read !== conversation) chatConversationRepository.save(read)

        val messages = chatMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId)

        return ConversationDetailResult.from(read, messages)
    }
}
