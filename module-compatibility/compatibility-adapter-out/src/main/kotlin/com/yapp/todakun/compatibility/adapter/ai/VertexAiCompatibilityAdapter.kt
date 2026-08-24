package com.yapp.todakun.compatibility.adapter.ai

import com.yapp.todakun.common.format.formatSajuPillar
import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.compatibility.exception.CompatibilityCircuitOpenException
import com.yapp.todakun.compatibility.exception.CompatibilityEmptyResponseException
import com.yapp.todakun.compatibility.exception.CompatibilityGenerationFailedException
import com.yapp.todakun.compatibility.exception.CompatibilityTimeoutException
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiInput
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiPort
import com.yapp.todakun.compatibility.port.outbound.CompatibilityChartProfile
import com.yapp.todakun.compatibility.port.outbound.CompatibilityPillar
import com.yapp.todakun.compatibility.port.outbound.GeneratedCompatibility
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions
import org.springframework.stereotype.Component

private const val AI_RESILIENCE_INSTANCE_NAME = "compatibility-ai"

// 프롬프트 지시문만으로는 JSON 형식이 강제되지 않아, Gemini가 문법적으로 깨진 JSON(예: 닫는 `}` 누락)을 응답할 수 있다.
// provider 단에서 JSON 출력 모드를 강제해 BeanOutputConverter 파싱 실패를 줄인다.
private val JSON_RESPONSE_OPTIONS = VertexAiGeminiChatOptions.builder().responseMimeType("application/json").build()

/**
 * Vertex AI(Gemini)로 두 명식의 궁합 총운을 생성하는 [CompatibilityAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedCompatibility]) 매핑을 전담한다. 오행 비율은 도메인이 결정적으로 계산하므로 생성하지 않는다.
 * AI 호출은 [AiResilienceSupport]로 CircuitBreaker+Retry+TimeLimiter를 적용한다.
 */
@Component
class VertexAiCompatibilityAdapter(
    chatClientBuilder: ChatClient.Builder,
    private val resilience: AiResilienceSupport,
) : CompatibilityAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(input: CompatibilityAiInput): GeneratedCompatibility {
        val generated =
            resilience.execute(
                AI_RESILIENCE_INSTANCE_NAME,
                onCircuitOpen = { CompatibilityCircuitOpenException(it) },
                onTimeout = { CompatibilityTimeoutException(it) },
                onFailure = { CompatibilityGenerationFailedException(it) },
            ) { callAi(input) }

        return generated ?: throw CompatibilityEmptyResponseException()
    }

    private fun callAi(input: CompatibilityAiInput): GeneratedCompatibility? =
        chatClient
            .prompt()
            .user(buildPrompt(input))
            .options(JSON_RESPONSE_OPTIONS)
            .call()
            .entity(GeneratedCompatibility::class.java)

    private fun buildPrompt(input: CompatibilityAiInput): String =
        """
        You are a content writer producing a "compatibility (궁합)" reading based on traditional Korean Saju (Four Pillars) astrology.
        Analyze the compatibility between "me" and "the partner". Their relationship is ${input.relationshipType.label}(${input.relationshipType.name}).

        [My Saju Chart]
        ${input.myProfile.describe()}

        [Partner's Saju Chart]
        ${input.partnerProfile.describe()}

        [Writing Rules]
        1. score: integer between 0 and 100, an overall compatibility score for the two people.
        2. headline: a short headline copy for the compatibility. 50 characters or fewer (e.g. "함께할수록 빛나는 궁합").
        3. subheadline: a one-line summary shown under the score. 100 characters or fewer (e.g. "함께 있을 때, 편안함이 커지는 사이예요.").
        4. summary: a brief supporting copy shown under the subheadline. 200 characters or fewer.
        5. totalAnalysis: the overall compatibility analysis. Base it on the 상생/상극 relationship between the two charts'
           day masters, four pillars, and five-element (오행) balance.
        6. Write all sentences in Korean, keeping a warm and positive tone while avoiding unfounded exaggeration.
           Refer to the partner politely (e.g. "상대방").
        """.trimIndent()

    private fun CompatibilityChartProfile.describe(): String =
        """
        - Day Master: $dayMaster
        - Year Pillar: ${yearPillar.describe()}
        - Month Pillar: ${monthPillar.describe()}
        - Day Pillar: ${dayPillar.describe()}
        - Hour Pillar: ${hourPillar?.describe() ?: "unknown"}
        - Ohaeng (Five Elements) distribution (character count): ${ohaeng.entries.joinToString { "${it.key} ${it.value}" }}
        - Sipseong (Ten Gods) distribution (character count): ${sipseong.entries.joinToString { "${it.key} ${it.value}" }}
        """.trimIndent()

    private fun CompatibilityPillar.describe(): String = formatSajuPillar(stem, branch, stemSipseong, branchSipseong, sibiunseong)
}
