package com.yapp.todakun.chat.port.outbound

import com.yapp.todakun.chat.ChatConversation
import java.util.UUID

/** 대화 스레드 영속화 아웃바운드 포트. */
interface ChatConversationRepository {
    fun save(conversation: ChatConversation): ChatConversation

    fun findById(id: UUID): ChatConversation?

    /** 회원의 전체 대화를 최근 활동([ChatConversation.lastMessageAt]) 내림차순으로 조회한다. */
    fun findAllByMemberIdOrderByLastMessageAtDesc(memberId: UUID): List<ChatConversation>

    fun deleteById(id: UUID)
}
