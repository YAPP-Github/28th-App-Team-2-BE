package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.exception.ChatAssistantMessageNotFoundException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class CompleteChatTurnServiceTest : DescribeSpec({
    val chatConversationRepository = mockk<ChatConversationRepository>()
    val chatMessageRepository = mockk<ChatMessageRepository>()
    val service = CompleteChatTurnService(chatConversationRepository, chatMessageRepository)

    afterTest { clearMocks(chatConversationRepository, chatMessageRepository) }

    describe("complete") {
        context("어시스턴트 메시지와 대화가 모두 존재하면") {
            it("메시지를 완료 상태로, 대화를 최신 활동/안읽음 상태로 갱신해 저장한다") {
                val conversationId = Uuid.generateV7().toJavaUuid()
                val assistantMessageId = Uuid.generateV7().toJavaUuid()
                val message =
                    ChatFixture.assistantMessage(
                        id = assistantMessageId,
                        conversationId = conversationId,
                        status = ChatMessageStatus.GENERATING,
                    )
                val conversation = ChatFixture.conversation(id = conversationId, unread = false)
                every { chatMessageRepository.findById(assistantMessageId) } returns message
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                every { chatConversationRepository.findById(conversationId) } returns conversation
                every { chatConversationRepository.save(any()) } answers { firstArg() }
                val action = ChatFixture.action()

                service.complete(conversationId, assistantMessageId, "완성된 답변", action, unread = true)

                verify(exactly = 1) {
                    chatMessageRepository.save(
                        match {
                            it.status == ChatMessageStatus.COMPLETED && it.content == "완성된 답변" && it.action == action
                        },
                    )
                }
                verify(exactly = 1) { chatConversationRepository.save(match { it.unread == true }) }
            }
        }

        context("어시스턴트 메시지가 존재하지 않으면") {
            it("ChatAssistantMessageNotFoundException을 던지고 대화는 조회하지 않는다") {
                val conversationId = Uuid.generateV7().toJavaUuid()
                val assistantMessageId = Uuid.generateV7().toJavaUuid()
                every { chatMessageRepository.findById(assistantMessageId) } returns null

                shouldThrow<ChatAssistantMessageNotFoundException> {
                    service.complete(conversationId, assistantMessageId, "답변", null, unread = false)
                }

                verify(exactly = 0) { chatConversationRepository.findById(any()) }
            }
        }

        context("대화가 존재하지 않으면") {
            it("메시지는 이미 저장된 뒤 ChatConversationNotFoundException을 던진다") {
                val conversationId = Uuid.generateV7().toJavaUuid()
                val assistantMessageId = Uuid.generateV7().toJavaUuid()
                val message = ChatFixture.assistantMessage(id = assistantMessageId, conversationId = conversationId)
                every { chatMessageRepository.findById(assistantMessageId) } returns message
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                every { chatConversationRepository.findById(conversationId) } returns null

                shouldThrow<ChatConversationNotFoundException> {
                    service.complete(conversationId, assistantMessageId, "답변", null, unread = false)
                }

                verify(exactly = 1) { chatMessageRepository.save(any()) }
                verify(exactly = 0) { chatConversationRepository.save(any()) }
            }
        }
    }
})
