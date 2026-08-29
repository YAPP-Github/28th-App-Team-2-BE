package com.yapp.todakun.chat.adapter.persistence

import com.yapp.todakun.chat.ChatConversation
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import org.springframework.stereotype.Repository
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

@Repository
class ChatConversationRepositoryAdapter(
    private val chatConversationJpaRepository: ChatConversationJpaRepository,
) : ChatConversationRepository {
    override fun save(conversation: ChatConversation): ChatConversation =
        chatConversationJpaRepository.save(ChatConversationJpaEntity.fromDomain(conversation)).toDomain()

    override fun findById(id: UUID): ChatConversation? = chatConversationJpaRepository.findById(id).getOrNull()?.toDomain()

    override fun findAllByMemberIdOrderByLastMessageAtDesc(memberId: UUID): List<ChatConversation> =
        chatConversationJpaRepository.findAllByMemberIdOrderByLastMessageAtDesc(memberId).map { it.toDomain() }

    override fun deleteById(id: UUID) = chatConversationJpaRepository.deleteById(id)
}
