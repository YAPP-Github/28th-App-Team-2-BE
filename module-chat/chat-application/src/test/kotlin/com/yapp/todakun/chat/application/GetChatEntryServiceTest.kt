package com.yapp.todakun.chat.application

import com.yapp.todakun.chat.ChatSuggestionCatalog
import com.yapp.todakun.chat.fixture.ChatFixture
import com.yapp.todakun.chat.port.outbound.ChatQuotaPort
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class GetChatEntryServiceTest : DescribeSpec({
    val chatQuotaPort = mockk<ChatQuotaPort>()
    val service = GetChatEntryService(chatQuotaPort)

    afterTest { clearMocks(chatQuotaPort) }

    describe("getEntry") {
        context("회원의 진입 화면을 조회하면") {
            it("추천 질문 칩 목록과 오늘 남은 무료 채팅 사용량을 반환한다") {
                val memberId = Uuid.generateV7().toJavaUuid()
                every { chatQuotaPort.getStatus(memberId) } returns ChatFixture.quotaStatus(used = 3)

                val result = service.getEntry(memberId)

                result.greeting shouldBe ChatSuggestionCatalog.GREETING
                result.suggestions shouldBe ChatSuggestionCatalog.suggestions
                result.quotaUsed shouldBe 3
                result.quotaLimit shouldBe 100000
            }
        }
    }
})
