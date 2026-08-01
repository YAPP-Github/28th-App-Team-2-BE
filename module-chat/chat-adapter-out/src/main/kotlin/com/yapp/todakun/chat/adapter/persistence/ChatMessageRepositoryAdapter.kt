package com.yapp.todakun.chat.adapter.persistence

import com.yapp.todakun.chat.ChatMessage
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class ChatMessageRepositoryAdapter(
    private val chatMessageJpaRepository: ChatMessageJpaRepository,
) : ChatMessageRepository {
    override fun save(message: ChatMessage): ChatMessage =
        chatMessageJpaRepository.save(ChatMessageJpaEntity.fromDomain(message)).toDomain()

    override fun findById(id: UUID): ChatMessage? = chatMessageJpaRepository.findById(id).getOrNull()?.toDomain()

    override fun findAllByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<ChatMessage> =
        chatMessageJpaRepository.findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId).map { it.toDomain() }

    override fun findRecentByConversationId(
        conversationId: UUID,
        limit: Int,
    ): List<ChatMessage> =
        chatMessageJpaRepository
            .findAllByConversationIdOrderByCreatedAtDescIdDesc(conversationId, PageRequest.of(0, limit))
            .map { it.toDomain() }

    override fun deleteAllByConversationId(conversationId: UUID) = chatMessageJpaRepository.deleteAllByConversationId(conversationId)
}
