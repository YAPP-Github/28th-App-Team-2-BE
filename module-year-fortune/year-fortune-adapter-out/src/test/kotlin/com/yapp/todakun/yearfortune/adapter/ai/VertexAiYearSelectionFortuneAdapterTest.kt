package com.yapp.todakun.yearfortune.adapter.ai

import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneCircuitOpenException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneEmptyResponseException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneGenerationFailedException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneTimeoutException
import com.yapp.todakun.yearfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.yearfortune.port.outbound.GeneratedYearSelectionFortune
import com.yapp.todakun.yearfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.yearfortune.port.outbound.Pillar
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

private const val AI_RESILIENCE_INSTANCE_NAME = "year-fortune-ai"

class VertexAiYearSelectionFortuneAdapterTest : DescribeSpec({
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
    val adapter = VertexAiYearSelectionFortuneAdapter(chatClientBuilder, resilience)

    val year = 2026
    val yearPillar = pillar(stem = "병", branch = "오")
    val profile = memberSajuProfile()

    afterTest { clearMocks(chatClient, requestSpec, callResponseSpec) }

    describe("generate") {
        context("AI가 정상적인 구조화 응답을 반환하면") {
            it("회원 정보를 담은 프롬프트로 AI를 호출하고 그 결과를 반환한다") {
                val promptSlot = stubChatClient(chatClient, requestSpec, callResponseSpec)
                val generated = generatedYearSelectionFortune()
                every { callResponseSpec.entity(GeneratedYearSelectionFortune::class.java) } returns generated

                val result = adapter.generate(profile, year, yearPillar)

                result shouldBe generated
                promptSlot.captured shouldContain profile.gender
                promptSlot.captured shouldContain profile.dayMaster
                promptSlot.captured shouldContain year.toString()
                promptSlot.captured shouldContain FortuneCategory.LOVE.label
                promptSlot.captured shouldContain FortuneCategory.MONEY.label
                verify(exactly = 1) { requestSpec.call() }
            }
        }

        context("AI 호출이 예외를 던지면") {
            it("YearSelectionFortuneGenerationFailedException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                val cause = NonTransientAiException("model call failed")
                every { callResponseSpec.entity(GeneratedYearSelectionFortune::class.java) } throws cause

                shouldThrow<YearSelectionFortuneGenerationFailedException> {
                    adapter.generate(profile, year, yearPillar)
                }.cause shouldBe cause
            }
        }

        context("AI가 빈 응답(null)을 반환하면") {
            it("YearSelectionFortuneEmptyResponseException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedYearSelectionFortune::class.java) } returns null

                shouldThrow<YearSelectionFortuneEmptyResponseException> {
                    adapter.generate(profile, year, yearPillar)
                }
            }
        }

        context("CircuitBreaker가 열려 있으면") {
            it("YearSelectionFortuneCircuitOpenException을 던진다") {
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
                    VertexAiYearSelectionFortuneAdapter(
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
                every { callResponseSpec.entity(GeneratedYearSelectionFortune::class.java) } throws cause

                repeat(2) {
                    shouldThrow<YearSelectionFortuneGenerationFailedException> {
                        openCircuitAdapter.generate(profile, year, yearPillar)
                    }
                }

                shouldThrow<YearSelectionFortuneCircuitOpenException> {
                    openCircuitAdapter.generate(profile, year, yearPillar)
                }
            }
        }

        context("TimeLimiter 타임아웃 시간 안에 AI 응답이 없으면") {
            it("YearSelectionFortuneTimeoutException을 던진다") {
                val timeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
                timeLimiterRegistry.timeLimiter(
                    AI_RESILIENCE_INSTANCE_NAME,
                    TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(200)).build(),
                )
                val timeoutAdapter =
                    VertexAiYearSelectionFortuneAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            CircuitBreakerRegistry.ofDefaults(),
                            RetryRegistry.ofDefaults(),
                            timeLimiterRegistry,
                            Executors.newFixedThreadPool(2),
                        ),
                    )
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedYearSelectionFortune::class.java) } answers {
                    Thread.sleep(5000)
                    generatedYearSelectionFortune()
                }

                shouldThrow<YearSelectionFortuneTimeoutException> {
                    timeoutAdapter.generate(profile, year, yearPillar)
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

private fun generatedYearSelectionFortune(): GeneratedYearSelectionFortune =
    GeneratedYearSelectionFortune(
        title = "새해에는 좋은 기회가 많이 찾아와요",
        content = "전반적으로 안정적인 한 해가 될 것으로 보입니다.",
        score = 80,
        fortuneCategories =
            listOf(
                GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
                GeneratedCategoryFortune(FortuneCategory.MONEY, star = 3),
                GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
            ),
    )
