package com.yapp.todakun.chat.fixture

import com.yapp.todakun.chat.ChatAction
import com.yapp.todakun.chat.ChatActionType
import com.yapp.todakun.chat.ChatConversation
import com.yapp.todakun.chat.ChatMessage
import com.yapp.todakun.chat.ChatMessageRole
import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.port.outbound.ChatHistoryTurn
import com.yapp.todakun.chat.port.outbound.ChatPillarContext
import com.yapp.todakun.chat.port.outbound.ChatProfileContext
import com.yapp.todakun.chat.port.outbound.ChatPromptContext
import com.yapp.todakun.chat.port.outbound.ChatQuotaStatus
import com.yapp.todakun.chat.port.outbound.ChatSajuContext
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
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

    /** [StreamChatAnswerService]가 AI 호출에 넘기는 프롬프트 컨텍스트(chat 도메인 타입) 기본값. */
    fun promptContext(
        history: List<ChatHistoryTurn> = emptyList(),
        question: String = "오늘 하루 어때요?",
    ): ChatPromptContext =
        ChatPromptContext(
            saju =
                ChatSajuContext(
                    dayMaster = "갑",
                    yearPillar = pillarContext(),
                    monthPillar = pillarContext(),
                    dayPillar = pillarContext(),
                    hourPillar = null,
                    ohaeng = mapOf("목" to 3),
                    sipseong = mapOf("비견" to 2),
                ),
            profile =
                ChatProfileContext(
                    name = "토닥이 사용자",
                    birthDate = LocalDate.of(1998, 3, 5),
                    gender = "MALE",
                    job = "WORKER",
                    relationshipStatus = "SOLO",
                ),
            history = history,
            question = question,
        )

    fun pillarContext(
        stem: String = "갑",
        branch: String = "자",
        stemSipseong: String? = "비견",
        branchSipseong: String = "정관",
        sibiunseong: String = "장생",
    ): ChatPillarContext =
        ChatPillarContext(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )

    /** [PrepareChatTurnService]가 의존하는 [com.yapp.todakun.shared.GetSajuChartPort] 스텁용 기본값. */
    fun sajuChartSummary(): SajuChartSummary =
        SajuChartSummary(
            dayMaster = "갑",
            yearPillar = pillarSummary(),
            monthPillar = pillarSummary(),
            dayPillar = pillarSummary(),
            hourPillar = null,
            ohaeng = mapOf("목" to 3),
            sipseong = mapOf("비견" to 2),
        )

    fun pillarSummary(
        stem: String = "갑",
        branch: String = "자",
        stemSipseong: String? = "비견",
        branchSipseong: String = "정관",
        sibiunseong: String = "장생",
    ): PillarSummary =
        PillarSummary(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )

    /** [PrepareChatTurnService]가 의존하는 [com.yapp.todakun.shared.GetMemberFortuneProfilePort] 스텁용 기본값. */
    fun memberFortuneProfile(): MemberFortuneProfile =
        MemberFortuneProfile(
            name = "홍길동",
            birthDate = LocalDate.of(1998, 3, 5),
            gender = "MALE",
            job = "WORKER",
            relationshipStatus = "SOLO",
        )
}
