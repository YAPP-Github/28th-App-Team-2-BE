package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.ChatConversation
import com.yapp.todakun.chat.ChatMessage
import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.port.inbound.SendChatMessageCommand
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatHistoryTurn
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.chat.port.outbound.ChatPillarContext
import com.yapp.todakun.chat.port.outbound.ChatProfileContext
import com.yapp.todakun.chat.port.outbound.ChatPromptContext
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import com.yapp.todakun.chat.port.outbound.ChatSajuContext
import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import java.time.Instant
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

private const val CONVERSATION_TITLE_MAX_LENGTH = 60
private const val HISTORY_LIMIT = 20

// 어시스턴트 답변은 길이 제한이 없으므로(자유 형식 생성 결과), 메시지 개수만 제한하면 프롬프트가 무한정 커질 수 있다.
// 최신 턴을 우선해 이 문자 수 예산 안에서만 히스토리를 채운다(대략적인 토큰 예산 근사치).
private const val HISTORY_CHAR_BUDGET = 4000

/**
 * 답변 스트리밍 준비 단계(짧은 트랜잭션). 쿼터 예약 → 대화 확보(신규/이어가기) → 사용자 메시지·어시스턴트 placeholder 저장 →
 * AI 프롬프트 컨텍스트 구성까지 처리한다. 실제 AI 스트리밍 호출은 트랜잭션 밖([StreamChatAnswerService])에서 이뤄진다.
 */
@CommandService
class PrepareChatTurnService(
    private val chatConversationRepository: ChatConversationRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val chatQuotaPort: ChatQuotaPort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
) {
    @ExperimentalUuidApi
    fun prepare(command: SendChatMessageCommand): PreparedChatTurn {
        val quota = chatQuotaPort.reserve(command.memberId)
        val conversation = resolveConversation(command)
        // 이번 질문을 저장하기 전에 히스토리를 먼저 읽어, 프롬프트의 history와 question이 중복되지 않게 한다.
        val history = buildHistory(conversation.id)

        val userMessage = chatMessageRepository.save(ChatMessage.createUser(conversation.id, command.content))
        val assistantPlaceholder = chatMessageRepository.save(ChatMessage.createAssistantPlaceholder(conversation.id))

        return PreparedChatTurn(
            memberId = command.memberId,
            conversationId = conversation.id,
            conversationTitle = conversation.title,
            userMessageId = userMessage.id,
            assistantMessageId = assistantPlaceholder.id,
            quota = quota,
            promptContext = buildPromptContext(command.memberId, history, command.content),
        )
    }

    @ExperimentalUuidApi
    private fun resolveConversation(command: SendChatMessageCommand): ChatConversation {
        command.conversationId?.let { id ->
            val conversation = chatConversationRepository.findById(id) ?: throw ChatConversationNotFoundException()
            if (conversation.memberId != command.memberId) throw ChatConversationForbiddenException()
            return conversation
        }

        val title = command.content.take(CONVERSATION_TITLE_MAX_LENGTH)

        return chatConversationRepository.save(ChatConversation.create(command.memberId, title, Instant.now()))
    }

    private fun buildHistory(conversationId: UUID): List<ChatHistoryTurn> {
        val turns =
            chatMessageRepository
                .findRecentByConversationId(conversationId, HISTORY_LIMIT)
                .asReversed()
                .filter { it.status == ChatMessageStatus.COMPLETED }
                .map { ChatHistoryTurn(role = it.role, content = it.content) }

        return trimToCharBudget(turns)
    }

    private fun trimToCharBudget(turns: List<ChatHistoryTurn>): List<ChatHistoryTurn> {
        var remaining = HISTORY_CHAR_BUDGET
        val kept = mutableListOf<ChatHistoryTurn>()

        for (turn in turns.asReversed()) {
            val cost = turn.content.length
            // 가장 최근 턴은 예산을 넘더라도 최소 하나는 남겨 직전 맥락이 완전히 사라지지 않게 한다.
            if (cost > remaining && kept.isNotEmpty()) break
            kept.add(turn)
            remaining -= cost
        }

        return kept.asReversed()
    }

    private fun buildPromptContext(
        memberId: UUID,
        history: List<ChatHistoryTurn>,
        question: String,
    ): ChatPromptContext =
        ChatPromptContext(
            saju = getSajuChartPort.getChart(memberId).toSajuContext(),
            profile = getMemberFortuneProfilePort.getProfile(memberId).toProfileContext(),
            history = history,
            question = question,
        )

    private fun SajuChartSummary.toSajuContext(): ChatSajuContext =
        ChatSajuContext(
            dayMaster = dayMaster,
            yearPillar = yearPillar.toPillarContext(),
            monthPillar = monthPillar.toPillarContext(),
            dayPillar = dayPillar.toPillarContext(),
            hourPillar = hourPillar?.toPillarContext(),
            ohaeng = ohaeng,
            sipseong = sipseong,
        )

    private fun PillarSummary.toPillarContext(): ChatPillarContext =
        ChatPillarContext(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )

    private fun MemberFortuneProfile.toProfileContext(): ChatProfileContext =
        ChatProfileContext(
            name = name,
            birthDate = birthDate,
            gender = gender,
            job = job,
            relationshipStatus = relationshipStatus,
        )
}
