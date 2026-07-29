package com.yapp.todakun.compatibility.adapter.ai

import com.yapp.todakun.compatibility.exception.CompatibilityEmptyResponseException
import com.yapp.todakun.compatibility.exception.CompatibilityGenerationFailedException
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiInput
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiPort
import com.yapp.todakun.compatibility.port.outbound.CompatibilityChartProfile
import com.yapp.todakun.compatibility.port.outbound.CompatibilityPillar
import com.yapp.todakun.compatibility.port.outbound.GeneratedCompatibility
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

/**
 * Vertex AI(Gemini)로 두 명식의 궁합 총운을 생성하는 [CompatibilityAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedCompatibility]) 매핑을 전담한다. 오행 비율은 도메인이 결정적으로 계산하므로 생성하지 않는다.
 */
@Component
class VertexAiCompatibilityAdapter(
    chatClientBuilder: ChatClient.Builder,
) : CompatibilityAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(input: CompatibilityAiInput): GeneratedCompatibility =
        runCatching { callAi(input) }
            .getOrElse { throw CompatibilityGenerationFailedException(it) }
            ?: throw CompatibilityEmptyResponseException()

    private fun callAi(input: CompatibilityAiInput): GeneratedCompatibility? =
        chatClient
            .prompt()
            .user(buildPrompt(input))
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

    private fun CompatibilityPillar.describe(): String {
        val stemPart = stemSipseong?.let { "천간 $it, " } ?: ""

        return "$stem$branch (${stemPart}지지 $branchSipseong, 십이운성 $sibiunseong)"
    }
}
