package com.yapp.todakun.chat.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatConversationJpaRepository : JpaRepository<ChatConversationJpaEntity, UUID> {
    fun findAllByMemberIdOrderByLastMessageAtDesc(memberId: UUID): List<ChatConversationJpaEntity>
}
