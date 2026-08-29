package com.yapp.todakun.chat.adapter.web.controller

import com.yapp.todakun.chat.adapter.web.ChatApi
import com.yapp.todakun.chat.adapter.web.dto.response.ChatEntryResponse
import com.yapp.todakun.chat.adapter.web.dto.response.ConversationDetailResponse
import com.yapp.todakun.chat.adapter.web.dto.response.ConversationListResponse
import com.yapp.todakun.chat.port.inbound.DeleteConversationUseCase
import com.yapp.todakun.chat.port.inbound.GetChatEntryUseCase
import com.yapp.todakun.chat.port.inbound.GetConversationsUseCase
import com.yapp.todakun.chat.port.inbound.OpenConversationUseCase
import com.yapp.todakun.web.response.CommonResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ChatController(
    private val getChatEntryUseCase: GetChatEntryUseCase,
    private val getConversationsUseCase: GetConversationsUseCase,
    private val openConversationUseCase: OpenConversationUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
) : ChatApi {
    override fun getEntry(memberId: UUID): ResponseEntity<CommonResponse<ChatEntryResponse>> =
        CommonResponse.retrieved(ChatEntryResponse.from(getChatEntryUseCase.getEntry(memberId)))

    override fun getConversations(memberId: UUID): ResponseEntity<CommonResponse<ConversationListResponse>> =
        CommonResponse.retrieved(ConversationListResponse.from(getConversationsUseCase.getConversations(memberId)))

    override fun getConversation(
        memberId: UUID,
        conversationId: UUID,
    ): ResponseEntity<CommonResponse<ConversationDetailResponse>> =
        CommonResponse.retrieved(ConversationDetailResponse.from(openConversationUseCase.open(memberId, conversationId)))

    override fun deleteConversation(
        memberId: UUID,
        conversationId: UUID,
    ): ResponseEntity<CommonResponse<Unit>> {
        deleteConversationUseCase.delete(memberId, conversationId)
        return CommonResponse.deleted()
    }
}
