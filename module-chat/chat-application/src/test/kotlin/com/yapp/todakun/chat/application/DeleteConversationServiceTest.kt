package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.exception.ChatConversationForbiddenException
import com.yapp.todakun.chat.exception.ChatConversationNotFoundException
import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class DeleteConversationServiceTest : DescribeSpec({
    val chatConversationRepository = mockk<ChatConversationRepository>()
    val chatMessageRepository = mockk<ChatMessageRepository>()
    val service = DeleteConversationService(chatConversationRepository, chatMessageRepository)

    afterTest { clearMocks(chatConversationRepository, chatMessageRepository) }

    describe("delete") {
        context("본인 소유의 대화를 삭제하면") {
            it("메시지를 먼저 삭제한 뒤 대화를 삭제한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                every { chatConversationRepository.findById(conversationId) } returns
                    ChatFixture.conversation(id = conversationId, memberId = memberId)
                every { chatMessageRepository.deleteAllByConversationId(conversationId) } just Runs
                every { chatConversationRepository.deleteById(conversationId) } just Runs

                service.delete(memberId, conversationId)

                verifyOrder {
                    chatMessageRepository.deleteAllByConversationId(conversationId)
                    chatConversationRepository.deleteById(conversationId)
                }
            }
        }

        context("존재하지 않는 대화면") {
            it("ChatConversationNotFoundException을 던지고 삭제를 수행하지 않는다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                every { chatConversationRepository.findById(conversationId) } returns null

                shouldThrow<ChatConversationNotFoundException> { service.delete(memberId, conversationId) }

                verify(exactly = 0) { chatMessageRepository.deleteAllByConversationId(any()) }
                verify(exactly = 0) { chatConversationRepository.deleteById(any()) }
            }
        }

        context("타인 소유 대화면") {
            it("ChatConversationForbiddenException을 던지고 삭제를 수행하지 않는다") {
                val ownerId = Uuid.generateV7().toJavaUuid()
                val requesterId = Uuid.generateV7().toJavaUuid()
                val conversationId = Uuid.generateV7().toJavaUuid()
                every { chatConversationRepository.findById(conversationId) } returns
                    ChatFixture.conversation(id = conversationId, memberId = ownerId)

                shouldThrow<ChatConversationForbiddenException> { service.delete(requesterId, conversationId) }

                verify(exactly = 0) { chatMessageRepository.deleteAllByConversationId(any()) }
                verify(exactly = 0) { chatConversationRepository.deleteById(any()) }
            }
        }
    }
})
