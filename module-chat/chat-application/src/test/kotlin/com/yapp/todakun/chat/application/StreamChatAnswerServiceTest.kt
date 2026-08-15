package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.inbound.ChatStreamListener
import com.yapp.todakun.chat.port.inbound.SendChatMessageCommand
import com.yapp.todakun.chat.port.outbound.ChatAiPort
import com.yapp.todakun.chat.port.outbound.ChatPillarContext
import com.yapp.todakun.chat.port.outbound.ChatProfileContext
import com.yapp.todakun.chat.port.outbound.ChatPromptContext
import com.yapp.todakun.chat.port.outbound.ChatQuotaStatus
import com.yapp.todakun.chat.port.outbound.ChatSajuContext
import com.yapp.todakun.shared.NotificationType
import com.yapp.todakun.shared.SendNotificationCommand
import com.yapp.todakun.shared.SendNotificationPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val PILLAR = ChatPillarContext(stem = "갑", branch = "자", stemSipseong = "비견", branchSipseong = "정관", sibiunseong = "장생")
private val PROMPT_CONTEXT =
    ChatPromptContext(
        saju =
            ChatSajuContext(
                dayMaster = "갑",
                yearPillar = PILLAR,
                monthPillar = PILLAR,
                dayPillar = PILLAR,
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
        history = emptyList(),
        question = "오늘 하루 어때요?",
    )

@ExperimentalUuidApi
class StreamChatAnswerServiceTest : DescribeSpec({
    val prepareChatTurnService = mockk<PrepareChatTurnService>()
    val completeChatTurnService = mockk<CompleteChatTurnService>()
    val failChatTurnService = mockk<FailChatTurnService>()
    val chatAiPort = mockk<ChatAiPort>()
    val sendNotificationPort = mockk<SendNotificationPort>()
    val listener = mockk<ChatStreamListener>()
    val service =
        StreamChatAnswerService(prepareChatTurnService, completeChatTurnService, failChatTurnService, chatAiPort, sendNotificationPort)

    afterTest {
        clearMocks(prepareChatTurnService, completeChatTurnService, failChatTurnService, chatAiPort, sendNotificationPort)
        clearMocks(listener, answers = false)
    }

    every { listener.onStart(any()) } just Runs
    every { listener.onDelta(any()) } just Runs
    every { listener.onAction(any()) } just Runs
    every { listener.onDone(any()) } just Runs
    every { listener.onError(any()) } just Runs

    val memberId = Uuid.generateV7().toJavaUuid()
    val conversationId = Uuid.generateV7().toJavaUuid()
    val userMessageId = Uuid.generateV7().toJavaUuid()
    val assistantMessageId = Uuid.generateV7().toJavaUuid()
    val command = SendChatMessageCommand(memberId, conversationId, "오늘 하루 어때요?")
    val prepared =
        PreparedChatTurn(
            memberId = memberId,
            conversationId = conversationId,
            conversationTitle = "오늘 하루 어때요?",
            userMessageId = userMessageId,
            assistantMessageId = assistantMessageId,
            quota = ChatQuotaStatus(used = 1, limit = 100000),
            promptContext = PROMPT_CONTEXT,
        )

    describe("stream") {
        context("준비 단계에서 실패하면") {
            it("listener.onError만 호출하고 이후 단계는 진행하지 않는다") {
                val cause = RuntimeException("prepare fail")
                every { prepareChatTurnService.prepare(command) } throws cause

                service.stream(command, listener)

                verify(exactly = 1) { listener.onError(cause) }
                verify(exactly = 0) { listener.onStart(any()) }
                verify(exactly = 0) { chatAiPort.streamAnswer(any(), any()) }
                verify(exactly = 0) { completeChatTurnService.complete(any(), any(), any(), any(), any()) }
                verify(exactly = 0) { failChatTurnService.fail(any(), any()) }
                verify(exactly = 0) { sendNotificationPort.send(any()) }
            }
        }

        context("AI 스트리밍 호출이 실패하면") {
            it("메시지를 실패 처리하고 listener.onError를 호출한다") {
                val cause = RuntimeException("ai fail")
                every { prepareChatTurnService.prepare(command) } returns prepared
                every { chatAiPort.streamAnswer(PROMPT_CONTEXT, any()) } throws cause
                every { failChatTurnService.fail(memberId, assistantMessageId) } just Runs

                service.stream(command, listener)

                verify(exactly = 1) { listener.onStart(any()) }
                verify(exactly = 1) { failChatTurnService.fail(memberId, assistantMessageId) }
                verify(exactly = 1) { listener.onError(cause) }
                verify(exactly = 0) { completeChatTurnService.complete(any(), any(), any(), any(), any()) }
                verify(exactly = 0) { sendNotificationPort.send(any()) }
            }
        }

        context("답변 완료 저장이 실패하면") {
            it("메시지를 실패 처리하고 listener.onError를 호출한다") {
                val cause = RuntimeException("save fail")
                every { prepareChatTurnService.prepare(command) } returns prepared
                every { chatAiPort.streamAnswer(PROMPT_CONTEXT, any()) } answers {
                    val onDelta = secondArg<(String) -> Unit>()
                    onDelta("안녕")
                    "안녕"
                }
                every { completeChatTurnService.complete(any(), any(), any(), any(), any()) } throws cause
                every { listener.isClientConnected() } returns true
                every { failChatTurnService.fail(memberId, assistantMessageId) } just Runs

                service.stream(command, listener)

                verify(exactly = 1) { listener.onDelta("안녕") }
                verify(exactly = 1) { failChatTurnService.fail(memberId, assistantMessageId) }
                verify(exactly = 1) { listener.onError(cause) }
                verify(exactly = 0) { sendNotificationPort.send(any()) }
            }
        }

        context("액션 추출이 실패해도") {
            it("답변 자체는 정상 완료되고 액션 없이 알림을 보낸다") {
                every { prepareChatTurnService.prepare(command) } returns prepared
                every { chatAiPort.streamAnswer(PROMPT_CONTEXT, any()) } answers {
                    val onDelta = secondArg<(String) -> Unit>()
                    onDelta("안녕하세요")
                    "안녕하세요"
                }
                every { chatAiPort.extractAction(PROMPT_CONTEXT, "안녕하세요") } throws RuntimeException("action fail")
                every { completeChatTurnService.complete(conversationId, assistantMessageId, "안녕하세요", null, false) } just Runs
                every { listener.isClientConnected() } returns true
                every { sendNotificationPort.send(any()) } just Runs

                service.stream(command, listener)

                verify(exactly = 1) { completeChatTurnService.complete(conversationId, assistantMessageId, "안녕하세요", null, false) }
                verify(exactly = 0) { listener.onAction(any()) }
                verify(exactly = 1) { listener.onDone(assistantMessageId) }
                verify(exactly = 0) { failChatTurnService.fail(any(), any()) }
            }
        }

        context("클라이언트가 연결을 유지 중이면") {
            it("읽음 처리하고 푸시 없이 알림을 보낸다") {
                every { prepareChatTurnService.prepare(command) } returns prepared
                every { chatAiPort.streamAnswer(PROMPT_CONTEXT, any()) } answers {
                    val onDelta = secondArg<(String) -> Unit>()
                    onDelta("답변")
                    "답변"
                }
                every { chatAiPort.extractAction(PROMPT_CONTEXT, "답변") } returns null
                every { completeChatTurnService.complete(conversationId, assistantMessageId, "답변", null, false) } just Runs
                every { listener.isClientConnected() } returns true
                val commandSlot = slot<SendNotificationCommand>()
                every { sendNotificationPort.send(capture(commandSlot)) } just Runs

                service.stream(command, listener)

                verify(exactly = 1) { completeChatTurnService.complete(conversationId, assistantMessageId, "답변", null, false) }
                commandSlot.captured.push shouldBe false
            }
        }

        context("클라이언트 연결이 끊겼으면") {
            it("안읽음 처리하고 푸시와 함께 알림을 보낸다") {
                every { prepareChatTurnService.prepare(command) } returns prepared
                every { chatAiPort.streamAnswer(PROMPT_CONTEXT, any()) } answers {
                    val onDelta = secondArg<(String) -> Unit>()
                    onDelta("답변")
                    "답변"
                }
                every { chatAiPort.extractAction(PROMPT_CONTEXT, "답변") } returns null
                every { completeChatTurnService.complete(conversationId, assistantMessageId, "답변", null, true) } just Runs
                every { listener.isClientConnected() } returns false
                val commandSlot = slot<SendNotificationCommand>()
                every { sendNotificationPort.send(capture(commandSlot)) } just Runs

                service.stream(command, listener)

                verify(exactly = 1) { completeChatTurnService.complete(conversationId, assistantMessageId, "답변", null, true) }
                commandSlot.captured.push shouldBe true
            }
        }

        context("답변이 정상 완료되면") {
            it("액션 카드를 전달하고, 알림 본문을 100자로 자르고, 딥링크를 채운다") {
                val action = ChatFixture.action()
                val longAnswer = "가".repeat(150)
                every { prepareChatTurnService.prepare(command) } returns prepared
                every { chatAiPort.streamAnswer(PROMPT_CONTEXT, any()) } returns longAnswer
                every { chatAiPort.extractAction(PROMPT_CONTEXT, longAnswer) } returns action
                every {
                    completeChatTurnService.complete(conversationId, assistantMessageId, longAnswer, action, false)
                } just Runs
                every { listener.isClientConnected() } returns true
                val commandSlot = slot<SendNotificationCommand>()
                every { sendNotificationPort.send(capture(commandSlot)) } just Runs

                service.stream(command, listener)

                verify(exactly = 1) {
                    listener.onStart(
                        match {
                            it.conversationId == conversationId &&
                                it.userMessageId == userMessageId &&
                                it.assistantMessageId == assistantMessageId &&
                                it.quotaUsed == prepared.quota.used &&
                                it.quotaLimit == prepared.quota.limit
                        },
                    )
                }
                verify(exactly = 1) { listener.onAction(action) }
                verify(exactly = 1) { listener.onDone(assistantMessageId) }
                commandSlot.captured.type shouldBe NotificationType.AI_COMPLETE
                commandSlot.captured.title shouldBe prepared.conversationTitle
                commandSlot.captured.content shouldBe longAnswer.take(100)
                commandSlot.captured.deepLink shouldBe "todakun://chat/conversations/$conversationId"
            }
        }
    }
})
