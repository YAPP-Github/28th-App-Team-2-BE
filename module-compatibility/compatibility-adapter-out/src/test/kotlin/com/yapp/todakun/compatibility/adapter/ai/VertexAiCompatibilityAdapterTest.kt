package com.yapp.todakun.compatibility.adapter.ai

import com.fasterxml.jackson.databind.ObjectMapper
import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.exception.CompatibilityCircuitOpenException
import com.yapp.todakun.compatibility.exception.CompatibilityEmptyResponseException
import com.yapp.todakun.compatibility.exception.CompatibilityGenerationFailedException
import com.yapp.todakun.compatibility.exception.CompatibilityTimeoutException
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiInput
import com.yapp.todakun.compatibility.port.outbound.CompatibilityChartProfile
import com.yapp.todakun.compatibility.port.outbound.CompatibilityPillar
import com.yapp.todakun.compatibility.port.outbound.GeneratedCompatibility
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
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions
import java.time.Duration
import java.util.concurrent.Executors

private const val AI_RESILIENCE_INSTANCE_NAME = "compatibility-ai"
private val objectMapper = ObjectMapper()

class VertexAiCompatibilityAdapterTest : DescribeSpec({
    val chatClientBuilder = mockk<ChatClient.Builder>()
    val chatClient = mockk<ChatClient>()
    val requestSpec = mockk<ChatClient.ChatClientRequestSpec>()
    val callResponseSpec = mockk<ChatClient.CallResponseSpec>()

    every { chatClientBuilder.build() } returns chatClient

    // 회로가 열리지 않도록 넉넉한 기본 임계값(레지스트리 기본 설정)을 쓰는 공용 어댑터 — 기존 정상/실패 흐름 검증용.
    val executorService = Executors.newFixedThreadPool(2)
    val resilience =
        AiResilienceSupport(
            CircuitBreakerRegistry.ofDefaults(),
            RetryRegistry.ofDefaults(),
            TimeLimiterRegistry.ofDefaults(),
        ) { executorService }
    val adapter = VertexAiCompatibilityAdapter(chatClientBuilder, resilience)
    val input = compatibilityAiInput()

    afterTest { clearMocks(chatClient, requestSpec, callResponseSpec) }
    afterSpec { executorService.shutdownNow() }

    describe("generate") {
        context("AI가 정상적인 구조화 응답을 반환하면") {
            it("두 명식과 관계 유형을 담은 프롬프트로 AI를 호출하고 그 결과를 반환한다") {
                val promptSlot = stubChatClient(chatClient, requestSpec, callResponseSpec)
                val generated = generatedCompatibility()
                every { callResponseSpec.entity(GeneratedCompatibility::class.java) } returns generated

                val result = adapter.generate(input)

                result shouldBe generated
                promptSlot.captured shouldContain input.relationshipType.label
                promptSlot.captured shouldContain input.myProfile.dayMaster
                verify(exactly = 1) { requestSpec.call() }
                verify(exactly = 1) {
                    requestSpec.options(
                        match<VertexAiGeminiChatOptions> {
                            it.responseMimeType == "application/json" && hasSchemaProperty(it.responseSchema, "headline")
                        },
                    )
                }
            }
        }

        context("AI 호출이 예외를 던지면") {
            it("CompatibilityGenerationFailedException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                val cause = NonTransientAiException("model call failed")
                every { callResponseSpec.entity(GeneratedCompatibility::class.java) } throws cause

                shouldThrow<CompatibilityGenerationFailedException> {
                    adapter.generate(input)
                }.cause shouldBe cause
            }
        }

        context("AI가 빈 응답(null)을 반환하면") {
            it("CompatibilityEmptyResponseException을 던진다") {
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedCompatibility::class.java) } returns null

                shouldThrow<CompatibilityEmptyResponseException> {
                    adapter.generate(input)
                }
            }
        }

        context("CircuitBreaker가 열려 있으면") {
            it("CompatibilityCircuitOpenException을 던진다") {
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
                val openCircuitExecutorService = Executors.newFixedThreadPool(2)
                val openCircuitAdapter =
                    VertexAiCompatibilityAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            circuitBreakerRegistry,
                            RetryRegistry.ofDefaults(),
                            TimeLimiterRegistry.ofDefaults(),
                        ) { openCircuitExecutorService },
                    )
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                val cause = NonTransientAiException("model call failed")
                every { callResponseSpec.entity(GeneratedCompatibility::class.java) } throws cause

                try {
                    repeat(2) {
                        shouldThrow<CompatibilityGenerationFailedException> {
                            openCircuitAdapter.generate(input)
                        }
                    }

                    shouldThrow<CompatibilityCircuitOpenException> {
                        openCircuitAdapter.generate(input)
                    }
                } finally {
                    openCircuitExecutorService.shutdownNow()
                }
            }
        }

        context("TimeLimiter 타임아웃 시간 안에 AI 응답이 없으면") {
            it("CompatibilityTimeoutException을 던진다") {
                val timeLimiterRegistry = TimeLimiterRegistry.ofDefaults()
                timeLimiterRegistry.timeLimiter(
                    AI_RESILIENCE_INSTANCE_NAME,
                    TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(200)).build(),
                )
                val timeoutExecutorService = Executors.newFixedThreadPool(2)
                val timeoutAdapter =
                    VertexAiCompatibilityAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            CircuitBreakerRegistry.ofDefaults(),
                            RetryRegistry.ofDefaults(),
                            timeLimiterRegistry,
                        ) { timeoutExecutorService },
                    )
                stubChatClient(chatClient, requestSpec, callResponseSpec)
                every { callResponseSpec.entity(GeneratedCompatibility::class.java) } answers {
                    Thread.sleep(5000)
                    generatedCompatibility()
                }

                try {
                    shouldThrow<CompatibilityTimeoutException> {
                        timeoutAdapter.generate(input)
                    }
                } finally {
                    timeoutExecutorService.shutdownNow()
                }
            }
        }
    }
})

private fun hasSchemaProperty(
    schema: String?,
    field: String,
): Boolean = schema?.let { objectMapper.readTree(it) }?.path("properties")?.has(field) == true

private fun stubChatClient(
    chatClient: ChatClient,
    requestSpec: ChatClient.ChatClientRequestSpec,
    callResponseSpec: ChatClient.CallResponseSpec,
): CapturingSlot<String> {
    val promptSlot = slot<String>()

    every { chatClient.prompt() } returns requestSpec
    every { requestSpec.user(capture(promptSlot)) } returns requestSpec
    every { requestSpec.options(any()) } returns requestSpec
    every { requestSpec.call() } returns callResponseSpec

    return promptSlot
}

private fun pillar(
    stem: String,
    branch: String,
    stemSipseong: String? = "비견",
    branchSipseong: String = "정관",
    sibiunseong: String = "장생",
): CompatibilityPillar =
    CompatibilityPillar(
        stem = stem,
        branch = branch,
        stemSipseong = stemSipseong,
        branchSipseong = branchSipseong,
        sibiunseong = sibiunseong,
    )

private fun chartProfile(dayMaster: String): CompatibilityChartProfile =
    CompatibilityChartProfile(
        dayMaster = dayMaster,
        yearPillar = pillar(stem = "신", branch = "사"),
        monthPillar = pillar(stem = "계", branch = "사"),
        dayPillar = pillar(stem = dayMaster, branch = "사", stemSipseong = null),
        hourPillar = null,
        ohaeng = mapOf("WATER" to 3, "FIRE" to 3),
        sipseong = mapOf("비견" to 2, "정관" to 1),
    )

private fun compatibilityAiInput(): CompatibilityAiInput =
    CompatibilityAiInput(
        relationshipType = CompatibilityRelationshipType.LOVER,
        myProfile = chartProfile(dayMaster = "계"),
        partnerProfile = chartProfile(dayMaster = "정"),
    )

private fun generatedCompatibility(): GeneratedCompatibility =
    GeneratedCompatibility(
        score = 85,
        headline = "함께할수록 빛나는 궁합",
        subheadline = "함께 있을 때, 편안함이 커지는 사이예요.",
        summary = "두 분은 서로의 부족한 기운을 보완합니다.",
        totalAnalysis = "따뜻한 기운과 유연한 기운이 만나 아름다운 관계를 이룹니다.",
    )
