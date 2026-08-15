package com.yapp.todakun.chat.fixture

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatActionType
import com.yapp.todakun.chat.ChatConversation
import com.yapp.todakun.chat.ChatMessage
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.port.outbound.ChatQuotaStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val USER_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")
private val ASSISTANT_MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000004")
private val DEFAULT_INSTANT = Instant.parse("2026-08-16T00:00:00Z")

// RedisChatQuotaAdapter의 DAILY_FREE_CHAT_LIMIT(테스트 편의를 위한 임시 핫픽스 상수)와 별개로,
// application 계층 테스트에서 쓰는 기본 한도값이다.
private const val DEFAULT_QUOTA_LIMIT = 100000

object ChatFixture {
    fun conversation(
        id: UUID = CONVERSATION_ID,
        memberId: UUID = MEMBER_ID,
        title: String = "오늘 하루 어때요?",
        unread: Boolean = false,
        lastMessageAt: Instant = DEFAULT_INSTANT,
        createdAt: Instant? = DEFAULT_INSTANT,
    ): ChatConversation = ChatConversation.reconstitute(id, memberId, title, unread, lastMessageAt, createdAt)

    fun userMessage(
        id: UUID = USER_MESSAGE_ID,
        conversationId: UUID = CONVERSATION_ID,
        content: String = "오늘 하루 어때요?",
        createdAt: Instant? = DEFAULT_INSTANT,
    ): ChatMessage =
        ChatMessage.reconstitute(
            id,
            conversationId,
            ChatMessageRole.USER,
            content,
            ChatMessageStatus.COMPLETED,
            null,
            createdAt,
        )

    fun assistantMessage(
        id: UUID = ASSISTANT_MESSAGE_ID,
        conversationId: UUID = CONVERSATION_ID,
        content: String = "",
        status: ChatMessageStatus = ChatMessageStatus.GENERATING,
        action: ChatAction? = null,
        createdAt: Instant? = DEFAULT_INSTANT,
    ): ChatMessage = ChatMessage.reconstitute(id, conversationId, ChatMessageRole.ASSISTANT, content, status, action, createdAt)

    fun action(
        type: ChatActionType = ChatActionType.CALENDAR_ADD,
        label: String = "이사 일정 추가하기",
        category: String = "계약・이사",
        date: LocalDate = LocalDate.of(2026, 8, 20),
    ): ChatAction = ChatAction(type = type, label = label, category = category, date = date)

    fun quotaStatus(
        used: Int = 1,
        limit: Int = DEFAULT_QUOTA_LIMIT,
    ): ChatQuotaStatus = ChatQuotaStatus(used = used, limit = limit)
}
