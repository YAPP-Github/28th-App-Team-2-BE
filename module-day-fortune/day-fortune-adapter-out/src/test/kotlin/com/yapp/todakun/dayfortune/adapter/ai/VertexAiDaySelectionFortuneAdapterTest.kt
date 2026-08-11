package com.yapp.todakun.dayfortune.adapter.ai

import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneCircuitOpenException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneEmptyResponseException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneGenerationFailedException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneTimeoutException
import com.yapp.todakun.dayfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dayfortune.port.outbound.GeneratedDaySelectionFortune
import com.yapp.todakun.dayfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dayfortune.port.outbound.Pillar
import com.yapp.todakun.shared.FortuneCategory
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
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
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.Executors

private const val AI_RESILIENCE_INSTANCE_NAME = "day-fortune-ai"

class VertexAiDaySelectionFortuneAdapterTest : DescribeSpec({
    val chatClientBuilder = mockk<ChatClient.Builder>()
    val chatClient = mockk<ChatClient>()
    val requestSpec = mockk<ChatClient.ChatClientRequestSpec>()
    val callResponseSpec = mockk<ChatClient.CallResponseSpec>()

    every { chatClientBuilder.build() } returns chatClient

    // 회로가 열리지 않도록 넉넉한 기본 임계값(레지스트리 기본 설정)을 쓰는 공용 어댑터 — 기존 정상/실패 흐름 검증용.
    val resilience =
        AiResilienceSupport(
            CircuitBreakerRegistry.ofDefaults(),
            RetryRegistry.ofDefaults(),
            TimeLimiterRegistry.ofDefaults(),
            Executors.newFixedThreadPool(2),
        )
    val adapter = VertexAiDaySelectionFortuneAdapter(chatClientBuilder, resilience)

    val purpose = DaySelectionPurpose.TRAVEL
    val targetDate = LocalDate.of(2026, 8, 1)
    val dayPillar = pillar(stem = "병", branch = "오")
    val profile = memberSajuProfile()

    afterTest { clearMocks(chatClient, requestSpec, callResponseSpec) }

    describe("generate") {
        context("AI가 정상적인 구조화 응답을 반환하면") {
            it("회원 정보를 담은 프롬프트로 AI를 호출하고 그 결과를 반환한다") {
                val promptSlot = stubChatClient(chatClient, requestSpec, callResponseSpec)
                val generated = generatedDaySelectionFortune()
                every { callResponseSpec.entity(GeneratedDaySelectionFortune::class.java) } returns generated

                val result = adapter.generate(profile, purpose, targetDate, dayPillar)

                result shouldBe generated
                promptSlot.captured shouldContain profile.gender
                promptSlot.captured shouldContain profile.dayMaster
                promptSlot.captured shouldContain targetDate.toString()
                promptSlot.captured shouldContain purpose.label
                promptSlot.captured shouldContain FortuneCategory.LOVE.label
                promptSlot.captured shouldContain FortuneCategory.MONEY.label
                verify(exactly = 1) { requestSpec.call() }
            }
        }

        context("AI 호출이 예외를 던지면") {
            it("DaySelectionFortuneGenerationFailedException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                val cause = NonTransientAiException("model call failed")
                every { callResponseSpec.entity(GeneratedDaySelectionFortune::class.java) } throws cause

                shouldThrow<DaySelectionFortuneGenerationFailedException> {
                    adapter.generate(profile, purpose, targetDate, dayPillar)
                }.cause shouldBe cause
            }
        }

        context("AI가 빈 응답(null)을 반환하면") {
            it("DaySelectionFortuneEmptyResponseException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedDaySelectionFortune::class.java) } returns null

                shouldThrow<DaySelectionFortuneEmptyResponseException> {
                    adapter.generate(profile, purpose, targetDate, dayPillar)
                }
            }
        }

        context("CircuitBreaker가 열려 있으면") {
            it("DaySelectionFortuneCircuitOpenException을 던진다") {
                val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
                circuitBreakerRegistry.circuitBreaker(
                    AI_RESILIENCE_INSTANCE_NAME,
                    CircuitBreakerConfig
                        .custom()
                        .slidingWindowSize(2)
                        .minimumNumberOfCalls(2)
                        .failureRateThreshold(50f)
                        .waitDurationInOpenState(Duration.ofSeconds(60))
                        .build(),
                )
                val openCircuitAdapter =
                    VertexAiDaySelectionFortuneAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            circuitBreakerRegistry,
                            RetryRegistry.ofDefaults(),
                            TimeLimiterRegistry.ofDefaults(),
                            Executors.newFixedThreadPool(2),
                        ),
                    )
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                val cause = NonTransientAiException("model call failed")
                every { callResponseSpec.entity(GeneratedDaySelectionFortune::class.java) } throws cause

                repeat(2) {
                    shouldThrow<DaySelectionFortuneGenerationFailedException> {
                        openCircuitAdapter.generate(profile, purpose, targetDate, dayPillar)
                    }
                }

                shouldThrow<DaySelectionFortuneCircuitOpenException> {
                    openCircuitAdapter.generate(profile, purpose, targetDate, dayPillar)
                }
            }
        }

        context("TimeLimiter 타임아웃 시간 안에 AI 응답이 없으면") {
            it("DaySelectionFortuneTimeoutException을 던진다") {
                val timeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
                timeLimiterRegistry.timeLimiter(
                    AI_RESILIENCE_INSTANCE_NAME,
                    TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(200)).build(),
                )
                val timeoutAdapter =
                    VertexAiDaySelectionFortuneAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            CircuitBreakerRegistry.ofDefaults(),
                            RetryRegistry.ofDefaults(),
                            timeLimiterRegistry,
                            Executors.newFixedThreadPool(2),
                        ),
                    )
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedDaySelectionFortune::class.java) } answers {
                    Thread.sleep(5000)
                    generatedDaySelectionFortune()
                }

                shouldThrow<DaySelectionFortuneTimeoutException> {
                    timeoutAdapter.generate(profile, purpose, targetDate, dayPillar)
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

private fun pillar(
    stem: String,
    branch: String,
    stemSipseong: String? = "비견",
    branchSipseong: String = "정관",
    sibiunseong: String = "장생",
): Pillar =
    Pillar(
        stem = stem,
        branch = branch,
        stemSipseong = stemSipseong,
        branchSipseong = branchSipseong,
        sibiunseong = sibiunseong,
    )

private fun memberSajuProfile(): MemberSajuProfile =
    MemberSajuProfile(
        birthDate = LocalDate.of(1998, 3, 5),
        gender = "MALE",
        job = "WORKER",
        relationshipStatus = "SOLO",
        fortuneCategories = FortuneCategory.entries.toList(),
        dayMaster = "갑",
        yearPillar = pillar(stem = "갑", branch = "자"),
        monthPillar = pillar(stem = "을", branch = "축"),
        dayPillar = pillar(stem = "갑", branch = "인", stemSipseong = null),
        hourPillar = null,
        ohaeng = mapOf("목" to 3, "화" to 2),
        sipseong = mapOf("비견" to 2, "정관" to 1),
    )

private fun generatedDaySelectionFortune(): GeneratedDaySelectionFortune =
    GeneratedDaySelectionFortune(
        title = "이 날은 여행하기 좋은 기운이 감돌아요",
        content = "전반적으로 안정적인 기운이 흐르는 날입니다.",
        score = 80,
        fortuneCategories =
            listOf(
                GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
                GeneratedCategoryFortune(FortuneCategory.MONEY, star = 3),
                GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
            ),
    )
