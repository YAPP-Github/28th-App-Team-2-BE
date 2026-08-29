package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class OpenConversationServiceTest : DescribeSpec({
    val chatConversationRepository = mockk<ChatConversationRepository>()
    val chatMessageRepository = mockk<ChatMessageRepository>()
    val service = OpenConversationService(chatConversationRepository, chatMessageRepository)

    afterTest { clearMocks(chatConversationRepository, chatMessageRepository) }

    describe("open") {
        context("안읽음 상태의 본인 대화를 열면") {
            it("읽음 처리해 저장하고 메시지 목록을 함께 반환한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val conversation = ChatFixture.conversation(id = conversationId, memberId = memberId, unread = true)
                val message = ChatFixture.userMessage(conversationId = conversationId)
                every { chatConversationRepository.findById(conversationId) } returns conversation
                every { chatConversationRepository.save(any()) } answers { firstArg() }
                every { chatMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId) } returns listOf(message)

                val result = service.open(memberId, conversationId)

                result.id shouldBe conversationId
                result.messages.map { it.id } shouldBe listOf(message.id)
                verify(exactly = 1) { chatConversationRepository.save(match { it.unread == false }) }
            }
        }

        context("이미 읽음 상태인 본인 대화를 열면") {
            it("저장을 호출하지 않고 메시지 목록만 반환한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                val conversation = ChatFixture.conversation(id = conversationId, memberId = memberId, unread = false)
                every { chatConversationRepository.findById(conversationId) } returns conversation
                every { chatMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(conversationId) } returns emptyList()

                service.open(memberId, conversationId)

                verify(exactly = 0) { chatConversationRepository.save(any()) }
            }
        }

        context("존재하지 않는 대화면") {
            it("ChatConversationNotFoundException을 던진다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                every { chatConversationRepository.findById(conversationId) } returns null

                shouldThrow<ChatConversationNotFoundException> { service.open(memberId, conversationId) }

                verify(exactly = 0) { chatMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(any()) }
            }
        }

        context("타인 소유 대화면") {
            it("ChatConversationForbiddenException을 던진다") {
                val ownerId = Uuid.generateV7().toJavaUuid()
                val requesterId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                every { chatConversationRepository.findById(conversationId) } returns
                    ChatFixture.conversation(id = conversationId, memberId = ownerId)

                shouldThrow<ChatConversationForbiddenException> { service.open(requesterId, conversationId) }

                verify(exactly = 0) { chatMessageRepository.findAllByConversationIdOrderByCreatedAtAsc(any()) }
            }
        }
    }
})
