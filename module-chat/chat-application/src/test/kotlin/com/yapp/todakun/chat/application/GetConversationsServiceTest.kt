package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.outbound.ChatConversationRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class GetConversationsServiceTest : DescribeSpec({
    val chatConversationRepository = mockk<ChatConversationRepository>()
    val service = GetConversationsService(chatConversationRepository)

    afterTest { clearMocks(chatConversationRepository) }

    describe("getConversations") {
        context("회원의 대화가 여러 건이면") {
            it("리포지토리가 반환한 순서(최근 활동 순)를 그대로 요약 목록으로 매핑한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                val newer = ChatFixture.conversation(id = Uuid.generateV7().toJavaUuid(), memberId = memberId, unread = true)
                val older = ChatFixture.conversation(id = Uuid.generateV7().toJavaUuid(), memberId = memberId)
                every { chatConversationRepository.findAllByMemberIdOrderByLastMessageAtDesc(memberId) } returns listOf(newer, older)

                val result = service.getConversations(memberId)

                result.conversations.map { it.id } shouldBe listOf(newer.id, older.id)
                result.conversations[0].unread shouldBe true
                result.conversations[0].title shouldBe newer.title
            }
        }

        context("대화가 하나도 없으면") {
            it("빈 목록을 반환한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                every { chatConversationRepository.findAllByMemberIdOrderByLastMessageAtDesc(memberId) } returns emptyList()

                service.getConversations(memberId).conversations.shouldBeEmpty()
            }
        }
    }
})
