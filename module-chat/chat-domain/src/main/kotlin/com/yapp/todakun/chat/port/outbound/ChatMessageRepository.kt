package com.yapp.todakun.chat.port.outbound

import com.yapp.todakun.chat.ChatMessage
import java.util.UUID

/** 대화 메시지 영속화 아웃바운드 포트. */
interface ChatMessageRepository {
    fun save(message: ChatMessage): ChatMessage

    fun findById(id: UUID): ChatMessage?

    /** 대화 상세 화면용 — 대화의 전체 메시지를 생성 순(오름차순)으로 조회한다. */
    fun findAllByConversationIdOrderByCreatedAtAsc(conversationId: UUID): List<ChatMessage>

    /** AI 프롬프트 컨텍스트용 — 최근 [limit]건을 최신순으로 조회한다(호출부가 시간순으로 뒤집어 사용). */
    fun findRecentByConversationId(
        conversationId: UUID,
        limit: Int,
    ): List<ChatMessage>

    fun deleteAllByConversationId(conversationId: UUID)
}
