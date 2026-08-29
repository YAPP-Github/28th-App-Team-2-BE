package com.yapp.todakun.chat.adapter.ai

import com.yapp.todakun.chat.exception.ChatGenerationFailedException
import com.yapp.todakun.chat.exception.ChatStreamUnavailableException
import com.yapp.todakun.chat.port.outbound.ChatPillarContext
import com.yapp.todakun.chat.port.outbound.ChatProfileContext
import com.yapp.todakun.chat.port.outbound.ChatPromptContext
import com.yapp.todakun.chat.port.outbound.ChatSajuContext
import com.yapp.todakun.common.resilience.AiResilienceSupport
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions
import reactor.core.publisher.Flux
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.Executors

private const val AI_RESILIENCE_INSTANCE_NAME = "chat-ai"
private val TODAY: LocalDate = LocalDate.of(2026, 8, 19)

class VertexAiChatAdapterTest : DescribeSpec({
    val chatClientBuilder = mockk<ChatClient.Builder>()
    val chatClient = mockk<ChatClient>()
    val requestSpec = mockk<ChatClient.ChatClientRequestSpec>()
    val streamResponseSpec = mockk<ChatClient.StreamResponseSpec>()

    every { chatClientBuilder.build() } returns chatClient

    // 회로가 열리지 않도록 넉넉한 기본 임계값(레지스트리 기본 설정)을 쓰는 공용 어댑터 — 기존 정상/실패 흐름 검증용.
    val resilience =
        AiResilienceSupport(
            CircuitBreakerRegistry.ofDefaults(),
            RetryRegistry.ofDefaults(),
            TimeLimiterRegistry.ofDefaults(),
        ) { Executors.newFixedThreadPool(2) }
    val adapter = VertexAiChatAdapter(chatClientBuilder, resilience)
    val context = chatPromptContext()

    afterTest { clearMocks(chatClient, requestSpec, streamResponseSpec) }

    describe("streamAnswer") {
        context("AI가 정상적으로 스트리밍 응답을 반환하면") {
            it("델타를 전달하고 전체 텍스트를 반환한다") {
                stubStreamingChatClient(chatClient, requestSpec, streamResponseSpec, Flux.just("안녕", "하세요"))
                val deltas = mutableListOf<String>()

                val result = adapter.streamAnswer(context) { deltas.add(it) }

                result shouldBe "안녕하세요"
                deltas shouldBe listOf("안녕", "하세요")
            }
        }

        context("AI 호출이 예외를 던지면") {
            it("ChatGenerationFailedException으로 감싸 던진다") {
                stubStreamingChatClient(chatClient, requestSpec, streamResponseSpec, Flux.error(RuntimeException("model call failed")))

                shouldThrow<ChatGenerationFailedException> {
                    adapter.streamAnswer(context) { }
                }
            }
        }

        context("CircuitBreaker가 열려 있으면") {
            it("ChatStreamUnavailableException을 던진다") {
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
                    VertexAiChatAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            circuitBreakerRegistry,
                            RetryRegistry.ofDefaults(),
                            TimeLimiterRegistry.ofDefaults(),
                        ) { Executors.newFixedThreadPool(2) },
                    )
                stubStreamingChatClient(chatClient, requestSpec, streamResponseSpec, Flux.error(RuntimeException("model call failed")))

                repeat(2) {
                    shouldThrow<ChatGenerationFailedException> {
                        openCircuitAdapter.streamAnswer(context) { }
                    }
                }

                shouldThrow<ChatStreamUnavailableException> {
                    openCircuitAdapter.streamAnswer(context) { }
                }
            }
        }
    }

    describe("extractAction") {
        context("AI가 정상적으로 액션 카드를 반환하면") {
            it("JSON 출력 모드를 강제해 호출한다") {
                val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
                every { chatClient.prompt() } returns requestSpec
                every { requestSpec.system(any<String>()) } returns requestSpec
                every { requestSpec.user(any<String>()) } returns requestSpec
                every { requestSpec.options(any()) } returns requestSpec
                every { requestSpec.call() } returns callResponseSpec
                every { callResponseSpec.entity(RawChatAction::class.java) } returns
                    RawChatAction(hasAction = false, type = null, label = null, category = null, date = null)

                adapter.extractAction(context, "답변").shouldBeNull()

                verify(exactly = 1) {
                    requestSpec.options(match<VertexAiGeminiChatOptions> { it.responseMimeType == "application/json" })
                }
            }
        }

        context("CircuitBreaker가 열려 있으면") {
            it("예외를 던지지 않고 null을 반환한다(부가 기능이므로 실패해도 답변 자체는 막지 않는다)") {
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
                    VertexAiChatAdapter(
                        chatClientBuilder,
                        AiResilienceSupport(
                            circuitBreakerRegistry,
                            RetryRegistry.ofDefaults(),
                            TimeLimiterRegistry.ofDefaults(),
                        ) { Executors.newFixedThreadPool(2) },
                    )
                val callResponseSpec = mockk<ChatClient.CallResponseSpec>()
                every { chatClient.prompt() } returns requestSpec
                every { requestSpec.system(any<String>()) } returns requestSpec
                every { requestSpec.user(any<String>()) } returns requestSpec
                every { requestSpec.options(any()) } returns requestSpec
                every { requestSpec.call() } returns callResponseSpec
                every { callResponseSpec.entity(RawChatAction::class.java) } throws RuntimeException("model call failed")

                repeat(2) { openCircuitAdapter.extractAction(context, "답변") }

                openCircuitAdapter.extractAction(context, "답변").shouldBeNull()
            }
        }
    }
})

private fun stubStreamingChatClient(
    chatClient: ChatClient,
    requestSpec: ChatClient.ChatClientRequestSpec,
    streamResponseSpec: ChatClient.StreamResponseSpec,
    content: Flux<String>,
) {
    every { chatClient.prompt() } returns requestSpec
    every { requestSpec.system(any<String>()) } returns requestSpec
    every { requestSpec.user(any<String>()) } returns requestSpec
    every { requestSpec.stream() } returns streamResponseSpec
    every { streamResponseSpec.content() } returns content
}

private fun pillar(
    stem: String,
    branch: String,
    stemSipseong: String? = "비견",
    branchSipseong: String = "정관",
    sibiunseong: String = "장생",
): ChatPillarContext =
    ChatPillarContext(
        stem = stem,
        branch = branch,
        stemSipseong = stemSipseong,
        branchSipseong = branchSipseong,
        sibiunseong = sibiunseong,
    )

private fun chatPromptContext(): ChatPromptContext =
    ChatPromptContext(
        today = TODAY,
        saju =
            ChatSajuContext(
                dayMaster = "갑",
                yearPillar = pillar(stem = "갑", branch = "자"),
                monthPillar = pillar(stem = "을", branch = "축"),
                dayPillar = pillar(stem = "갑", branch = "인", stemSipseong = null),
                hourPillar = null,
                ohaeng = mapOf("목" to 3, "화" to 2),
                sipseong = mapOf("비견" to 2, "정관" to 1),
            ),
        profile =
            ChatProfileContext(
                name = "토닥이 사용자",
                birthDate = LocalDate.of(1998, 3, 5),
                gender = "MALE",
                job = "WORKER",
                relationshipStatus = "SOLO",
            ),
        history = emptyList(),
        question = "오늘 하루 어때요?",
    )
