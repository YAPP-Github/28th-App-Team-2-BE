package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.exception.ChatDailyQuotaExceededException
import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.inbound.SendChatMessageCommand
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val PILLAR = PillarSummary(stem = "갑", branch = "자", stemSipseong = "비견", branchSipseong = "정관", sibiunseong = "장생")
private val SAJU_CHART =
    SajuChartSummary(
        dayMaster = "갑",
        yearPillar = PILLAR,
        monthPillar = PILLAR,
        dayPillar = PILLAR,
        hourPillar = null,
        ohaeng = mapOf("목" to 3),
        sipseong = mapOf("비견" to 2),
    )
private val PROFILE =
    MemberFortuneProfile(name = "홍길동", birthDate = LocalDate.of(1998, 3, 5), gender = "MALE", job = "WORKER", relationshipStatus = "SOLO")

@ExperimentalUuidApi
class PrepareChatTurnServiceTest : DescribeSpec({
    val chatConversationRepository = mockk<ChatConversationRepository>()
    val chatMessageRepository = mockk<ChatMessageRepository>()
    val chatQuotaPort = mockk<ChatQuotaPort>()
    val getSajuChartPort = mockk<GetSajuChartPort>()
    val getMemberFortuneProfilePort = mockk<GetMemberFortuneProfilePort>()
    val service =
        PrepareChatTurnService(
            chatConversationRepository,
            chatMessageRepository,
            chatQuotaPort,
            getSajuChartPort,
            getMemberFortuneProfilePort,
        )

    afterTest {
        clearMocks(chatConversationRepository, chatMessageRepository, chatQuotaPort, getSajuChartPort, getMemberFortuneProfilePort)
    }

    fun stubCrossDomain(memberId: java.util.UUID) {
        every { getSajuChartPort.getChart(memberId) } returns SAJU_CHART
        every { getMemberFortuneProfilePort.getProfile(memberId) } returns PROFILE
    }

    describe("prepare") {
        context("conversationId가 없으면") {
            it("새 대화를 생성하고 사용자·어시스턴트 메시지를 저장한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val command = SendChatMessageCommand(memberId, null, "오늘 하루 어때요?")
                every { chatQuotaPort.reserve(memberId) } returns ChatFixture.quotaStatus(used = 1)
                every { chatConversationRepository.save(any()) } answers { firstArg() }
                every { chatMessageRepository.findRecentByConversationId(any(), any()) } returns emptyList()
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                stubCrossDomain(memberId)

                val result = service.prepare(command)

                result.memberId shouldBe memberId
                result.conversationTitle shouldBe "오늘 하루 어때요?"
                result.promptContext.question shouldBe "오늘 하루 어때요?"
                result.promptContext.history.shouldBeEmpty()
                result.promptContext.saju.dayMaster shouldBe SAJU_CHART.dayMaster
                result.promptContext.profile.name shouldBe PROFILE.name
                verify(exactly = 1) { chatConversationRepository.save(any()) }
                verify(exactly = 0) { chatConversationRepository.findById(any()) }
                verify(exactly = 0) { chatQuotaPort.refund(any()) }
            }
        }

        context("conversationId가 있고 본인 소유면") {
            it("새 대화를 만들지 않고 기존 대화를 이어간다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val conversation = ChatFixture.conversation(id = conversationId, memberId = memberId, title = "이전 대화")
                val command = SendChatMessageCommand(memberId, conversationId, "다음 질문")
                every { chatQuotaPort.reserve(memberId) } returns ChatFixture.quotaStatus(used = 2)
                every { chatConversationRepository.findById(conversationId) } returns conversation
                every { chatMessageRepository.findRecentByConversationId(conversationId, 20) } returns emptyList()
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                stubCrossDomain(memberId)

                val result = service.prepare(command)

                result.conversationId shouldBe conversationId
                result.conversationTitle shouldBe "이전 대화"
                verify(exactly = 0) { chatConversationRepository.save(any()) }
            }
        }

        context("conversationId가 있지만 존재하지 않으면") {
            it("ChatConversationNotFoundException을 던지고 쿼터를 환불한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val command = SendChatMessageCommand(memberId, conversationId, "질문")
                every { chatQuotaPort.reserve(memberId) } returns ChatFixture.quotaStatus(used = 1)
                every { chatConversationRepository.findById(conversationId) } returns null
                every { chatQuotaPort.refund(memberId) } just Runs

                shouldThrow<ChatConversationNotFoundException> { service.prepare(command) }

                verify(exactly = 1) { chatQuotaPort.refund(memberId) }
            }
        }

        context("conversationId가 타인 소유면") {
            it("ChatConversationForbiddenException을 던지고 쿼터를 환불한다") {
                val ownerId = Uuid.generateV7().toJavaUuid()
                val requesterId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val command = SendChatMessageCommand(requesterId, conversationId, "질문")
                every { chatQuotaPort.reserve(requesterId) } returns ChatFixture.quotaStatus(used = 1)
                every { chatConversationRepository.findById(conversationId) } returns
                    ChatFixture.conversation(id = conversationId, memberId = ownerId)
                every { chatQuotaPort.refund(requesterId) } just Runs

                shouldThrow<ChatConversationForbiddenException> { service.prepare(command) }

                verify(exactly = 1) { chatQuotaPort.refund(requesterId) }
            }
        }

        context("쿼터 예약 자체가 실패하면") {
            it("환불을 호출하지 않고 예외를 그대로 던진다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val command = SendChatMessageCommand(memberId, null, "질문")
                every { chatQuotaPort.reserve(memberId) } throws ChatDailyQuotaExceededException()

                shouldThrow<ChatDailyQuotaExceededException> { service.prepare(command) }

                verify(exactly = 0) { chatQuotaPort.refund(any()) }
                verify(exactly = 0) { chatConversationRepository.findById(any()) }
            }
        }

        context("히스토리에 진행 중/실패 상태 메시지가 섞여 있으면") {
            it("완료된 메시지만 히스토리에 포함한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val conversation = ChatFixture.conversation(id = conversationId, memberId = memberId)
                val command = SendChatMessageCommand(memberId, conversationId, "다음 질문")
                val completed = ChatFixture.userMessage(conversationId = conversationId, content = "이전 질문")
                val generating =
                    ChatFixture.assistantMessage(
                        id = Uuid.generateV7().toJavaUuid(),
                        conversationId = conversationId,
                        status = ChatMessageStatus.GENERATING,
                    )
                every { chatQuotaPort.reserve(memberId) } returns ChatFixture.quotaStatus(used = 1)
                every { chatConversationRepository.findById(conversationId) } returns conversation
                // findRecentByConversationId는 최신순으로 반환한다(호출부가 뒤집어 사용) — 진행 중 메시지가 더 최근이다.
                every { chatMessageRepository.findRecentByConversationId(conversationId, 20) } returns listOf(generating, completed)
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                stubCrossDomain(memberId)

                val result = service.prepare(command)

                result.promptContext.history.map { it.content } shouldBe listOf("이전 질문")
            }
        }

        context("히스토리 문자 수 총합이 예산을 초과하면") {
            it("오래된 턴부터 버리고 최신 턴 위주로 채운다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val conversation = ChatFixture.conversation(id = conversationId, memberId = memberId)
                val command = SendChatMessageCommand(memberId, conversationId, "다음 질문")
                val older = ChatFixture.userMessage(conversationId = conversationId, content = "가".repeat(3500))
                val newer =
                    ChatFixture.assistantMessage(
                        id = Uuid.generateV7().toJavaUuid(),
                        conversationId = conversationId,
                        content = "나".repeat(3500),
                        status = ChatMessageStatus.COMPLETED,
                    )
                every { chatQuotaPort.reserve(memberId) } returns ChatFixture.quotaStatus(used = 1)
                every { chatConversationRepository.findById(conversationId) } returns conversation
                // 최신순 반환: newer가 older보다 먼저 온다.
                every { chatMessageRepository.findRecentByConversationId(conversationId, 20) } returns listOf(newer, older)
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                stubCrossDomain(memberId)

                val result = service.prepare(command)

                result.promptContext.history.map { it.content } shouldBe listOf(newer.content)
            }
        }

        context("가장 최신 턴 하나만으로도 예산을 초과하면") {
            it("그래도 최소 하나는 히스토리에 남긴다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val conversation = ChatFixture.conversation(id = conversationId, memberId = memberId)
                val command = SendChatMessageCommand(memberId, conversationId, "다음 질문")
                val onlyTurn = ChatFixture.userMessage(conversationId = conversationId, content = "가".repeat(5000))
                every { chatQuotaPort.reserve(memberId) } returns ChatFixture.quotaStatus(used = 1)
                every { chatConversationRepository.findById(conversationId) } returns conversation
                every { chatMessageRepository.findRecentByConversationId(conversationId, 20) } returns listOf(onlyTurn)
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                stubCrossDomain(memberId)

                val result = service.prepare(command)

                result.promptContext.history.map { it.content } shouldBe listOf(onlyTurn.content)
            }
        }
    }
})
