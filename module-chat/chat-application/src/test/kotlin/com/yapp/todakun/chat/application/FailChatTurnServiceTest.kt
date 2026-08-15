package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.ChatMessageStatus
import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.outbound.ChatMessageRepository
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class FailChatTurnServiceTest : DescribeSpec({
    val chatMessageRepository = mockk<ChatMessageRepository>()
    val chatQuotaPort = mockk<ChatQuotaPort>()
    val service = FailChatTurnService(chatMessageRepository, chatQuotaPort)

    afterTest { clearMocks(chatMessageRepository, chatQuotaPort) }

    describe("fail") {
        context("어시스턴트 메시지가 존재하면") {
            it("메시지를 실패 상태로 저장하고 쿼터를 환불한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val assistantMessageId = Uuid.generateV7().toJavaUuid()
                val message = ChatFixture.assistantMessage(id = assistantMessageId, status = ChatMessageStatus.GENERATING)
                every { chatMessageRepository.findById(assistantMessageId) } returns message
                every { chatMessageRepository.save(any()) } answers { firstArg() }
                every { chatQuotaPort.refund(memberId) } just Runs

                service.fail(memberId, assistantMessageId)

                verify(exactly = 1) { chatMessageRepository.save(match { it.status == ChatMessageStatus.FAILED }) }
                verify(exactly = 1) { chatQuotaPort.refund(memberId) }
            }
        }

        context("어시스턴트 메시지가 이미 사라졌으면(대화 삭제 등)") {
            it("저장은 건너뛰고 쿼터는 그대로 환불한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val assistantMessageId = Uuid.generateV7().toJavaUuid()
                every { chatMessageRepository.findById(assistantMessageId) } returns null
                every { chatQuotaPort.refund(memberId) } just Runs

                service.fail(memberId, assistantMessageId)

                verify(exactly = 0) { chatMessageRepository.save(any()) }
                verify(exactly = 1) { chatQuotaPort.refund(memberId) }
            }
        }

        context("메시지 저장 자체가 예외를 던져도") {
            it("예외는 그대로 전파되지만 쿼터 환불은 실행된다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val assistantMessageId = Uuid.generateV7().toJavaUuid()
                val message = ChatFixture.assistantMessage(id = assistantMessageId)
                every { chatMessageRepository.findById(assistantMessageId) } returns message
                every { chatMessageRepository.save(any()) } throws RuntimeException("db error")
                every { chatQuotaPort.refund(memberId) } just Runs

                shouldThrow<RuntimeException> { service.fail(memberId, assistantMessageId) }

                verify(exactly = 1) { chatQuotaPort.refund(memberId) }
            }
        }
    }
})
