package com.yapp.todakun.chat.adapter.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatMessageJpaRepository : JpaRepository<ChatMessageJpaEntity, UUID> {
    fun findAllByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<ChatMessageJpaEntity>

    fun findAllByConversationIdOrderByCreatedAtDesc(
        conversationId: UUID,
        pageable: Pageable,
    ): List<ChatMessageJpaEntity>

    fun deleteAllByConversationId(conversationId: UUID)
}
