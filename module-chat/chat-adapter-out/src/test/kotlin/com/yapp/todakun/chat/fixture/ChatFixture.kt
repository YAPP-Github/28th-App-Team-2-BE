package com.yapp.todakun.chat.fixture

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatActionType
import com.yapp.todakun.chat.ChatConversation
import com.yapp.todakun.chat.ChatMessage
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.ChatMessageStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001")
private val CONVERSATION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")
private val MESSAGE_ID = UUID.fromString("018f0000-0000-7000-8000-000000000003")
private val DEFAULT_INSTANT = Instant.parse("2026-08-16T00:00:00Z")

object ChatFixture {
    fun conversation(
        id: UUID = CONVERSATION_ID,
        memberId: UUID = MEMBER_ID,
        title: String = "오늘 하루 어때요?",
        unread: Boolean = false,
        lastMessageAt: Instant = DEFAULT_INSTANT,
        createdAt: Instant? = DEFAULT_INSTANT,
    ): ChatConversation = ChatConversation.reconstitute(id, memberId, title, unread, lastMessageAt, createdAt)

    fun message(
        id: UUID = MESSAGE_ID,
        conversationId: UUID = CONVERSATION_ID,
        role: ChatMessageRole = ChatMessageRole.USER,
        content: String = "오늘 하루 어때요?",
        status: ChatMessageStatus = ChatMessageStatus.COMPLETED,
        action: ChatAction? = null,
        createdAt: Instant? = DEFAULT_INSTANT,
    ): ChatMessage = ChatMessage.reconstitute(id, conversationId, role, content, status, action, createdAt)

    fun action(
        type: ChatActionType = ChatActionType.CALENDAR_ADD,
        label: String = "이사 일정 추가하기",
        category: String = "계약・이사",
        date: LocalDate = LocalDate.of(2026, 8, 20),
    ): ChatAction = ChatAction(type = type, label = label, category = category, date = date)
}
