package com.yapp.todakun.chat.adapter.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ChatMessageJpaRepository : JpaRepository<ChatMessageJpaEntity, UUID> {
    // 사용자 메시지·어시스턴트 placeholder는 같은 준비 단계에서 연속 저장되어 createdAt이 동률일 수 있다 —
    // UUID v7(id)가 시간 순서를 보존하므로 보조 정렬 키로 사용해 순서를 결정적으로 만든다.
    fun findAllByConversationIdOrderByCreatedAtAscIdAsc(conversationId: UUID): List<ChatMessageJpaEntity>

    fun findAllByConversationIdOrderByCreatedAtDescIdDesc(
        conversationId: UUID,
        pageable: Pageable,
    ): List<ChatMessageJpaEntity>

    @Modifying
    @Query("delete from ChatMessageJpaEntity m where m.conversationId = :conversationId")
    fun deleteAllByConversationId(
        @Param("conversationId") conversationId: UUID,
    )
}
