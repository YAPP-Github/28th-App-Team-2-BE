package com.yapp.todakun.dailyfortune.adapter.ai

import com.yapp.todakun.dailyfortune.exception.DailyFortuneEmptyResponseException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneAiFixture
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.CapturingSlot
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.retry.NonTransientAiException
import java.time.LocalDate

class VertexAiDailyFortuneAdapterTest : DescribeSpec({
    val chatClientBuilder = mockk<ChatClient.Builder>()
    val chatClient = mockk<ChatClient>()
    val requestSpec = mockk<ChatClient.ChatClientRequestSpec>()
    val callResponseSpec = mockk<ChatClient.CallResponseSpec>()

    every { chatClientBuilder.build() } returns chatClient

    val adapter = VertexAiDailyFortuneAdapter(chatClientBuilder)

    val fortuneDate = LocalDate.of(2026, 7, 28)
    val todayPillar = DailyFortuneAiFixture.pillar(stem = "병", branch = "오")
    val profile = DailyFortuneAiFixture.memberSajuProfile()

    afterTest { clearMocks(chatClient, requestSpec, callResponseSpec) }

    describe("generate") {
        context("AI가 정상적인 구조화 응답을 반환하면") {
            it("회원 정보를 담은 프롬프트로 AI를 호출하고 그 결과를 반환한다") {
                val promptSlot = stubChatClient(chatClient, requestSpec, callResponseSpec)
                val generated = DailyFortuneAiFixture.generatedDailyFortune()
                every { callResponseSpec.entity(GeneratedDailyFortune::class.java) } returns generated

                val result = adapter.generate(profile, fortuneDate, todayPillar)

                result shouldBe generated
                promptSlot.captured shouldContain profile.name
                promptSlot.captured shouldContain profile.dayMaster
                promptSlot.captured shouldContain fortuneDate.toString()
                verify(exactly = 1) { requestSpec.call() }
            }
        }

        context("AI 호출이 예외를 던지면") {
            it("DailyFortuneGenerationFailedException으로 감싸 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                val cause = NonTransientAiException("model call failed")
                every { callResponseSpec.entity(GeneratedDailyFortune::class.java) } throws cause

                shouldThrow<DailyFortuneGenerationFailedException> {
                    adapter.generate(profile, fortuneDate, todayPillar)
                }.cause shouldBe cause
            }
        }

        context("AI가 빈 응답(null)을 반환하면") {
            it("DailyFortuneEmptyResponseException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedDailyFortune::class.java) } returns null

                shouldThrow<DailyFortuneEmptyResponseException> {
                    adapter.generate(profile, fortuneDate, todayPillar)
                }
            }
        }
    }
})

private fun stubChatClient(
    chatClient: ChatClient,
    requestSpec: ChatClient.ChatClientRequestSpec,
    callResponseSpec: ChatClient.CallResponseSpec,
): CapturingSlot<String> {
    val promptSlot = slot<String>()

    every { chatClient.prompt() } returns requestSpec
    every { requestSpec.user(capture(promptSlot)) } returns requestSpec
    every { requestSpec.call() } returns callResponseSpec

    return promptSlot
}
