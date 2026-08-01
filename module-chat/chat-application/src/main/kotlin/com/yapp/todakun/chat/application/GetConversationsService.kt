package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.port.inbound.ConversationListResult
import com.yapp.todakun.chat.port.inbound.ConversationSummaryResult
import com.yapp.todakun.chat.port.inbound.GetConversationsUseCase
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.common.annotation.QueryService
import java.util.UUID

@QueryService
class GetConversationsService(
    private val chatConversationRepository: ChatConversationRepository,
) : GetConversationsUseCase {
    override fun getConversations(memberId: UUID): ConversationListResult {
        val conversations =
            chatConversationRepository
                .findAllByMemberIdOrderByLastMessageAtDesc(memberId)
                .map(ConversationSummaryResult::from)

        return ConversationListResult(conversations)
    }
}
