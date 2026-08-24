package com.yapp.todakun.yearfortune.adapter.ai

import com.yapp.todakun.common.format.formatSajuPillar
import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneCircuitOpenException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneEmptyResponseException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneGenerationFailedException
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneTimeoutException
import com.yapp.todakun.yearfortune.port.outbound.GeneratedYearSelectionFortune
import com.yapp.todakun.yearfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.yearfortune.port.outbound.Pillar
import com.yapp.todakun.yearfortune.port.outbound.YearSelectionFortuneAiPort
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.converter.BeanOutputConverter
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions
import org.springframework.stereotype.Component

private const val AI_RESILIENCE_INSTANCE_NAME = "year-fortune-ai"

// 프롬프트 지시문만으로는 JSON 형식·구조가 강제되지 않아, Gemini가 문법적으로 깨진 JSON을 응답하거나 [GeneratedYearSelectionFortune]와 다른 구조(필드 누락, 타입 불일치)로 응답할 수 있다.
// provider 단에서 JSON 출력 모드 + entity() 변환 대상과 동일한 스키마를 강제해 BeanOutputConverter 파싱 실패를 줄인다.
private val JSON_RESPONSE_OPTIONS =
    VertexAiGeminiChatOptions.builder()
        .responseMimeType("application/json")
        .responseSchema(BeanOutputConverter(GeneratedYearSelectionFortune::class.java).jsonSchema)
        .build()

/**
 * Vertex AI(Gemini)로 연도별 운세를 생성하는 [YearSelectionFortuneAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedYearSelectionFortune]) 매핑을 전담한다.
 * AI 호출은 [AiResilienceSupport]로 CircuitBreaker+Retry+TimeLimiter를 적용한다.
 */
@Component
class VertexAiYearSelectionFortuneAdapter(
    chatClientBuilder: ChatClient.Builder,
    private val resilience: AiResilienceSupport,
) : YearSelectionFortuneAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(
        profile: MemberSajuProfile,
        year: Int,
        yearPillar: Pillar,
    ): GeneratedYearSelectionFortune {
        val generated =
            resilience.execute(
                AI_RESILIENCE_INSTANCE_NAME,
                onCircuitOpen = { YearSelectionFortuneCircuitOpenException(it) },
                onTimeout = { YearSelectionFortuneTimeoutException(it) },
                onFailure = { YearSelectionFortuneGenerationFailedException(it) },
            ) { callAi(profile, year, yearPillar) }

        return generated ?: throw YearSelectionFortuneEmptyResponseException()
    }

    private fun callAi(
        profile: MemberSajuProfile,
        year: Int,
        yearPillar: Pillar,
    ): GeneratedYearSelectionFortune? =
        chatClient
            .prompt()
            .user(buildPrompt(profile, year, yearPillar))
            .options(JSON_RESPONSE_OPTIONS)
            .call()
            .entity(GeneratedYearSelectionFortune::class.java)

    private fun buildPrompt(
        profile: MemberSajuProfile,
        year: Int,
        yearPillar: Pillar,
    ): String =
        """
        You are a content writer producing a "year fortune" reading based on traditional Korean Saju (Four Pillars) astrology.
        Using the member info below, write the fortune for the year $year.

        [Member Info]
        - Birth date: ${profile.birthDate}
        - Gender: ${profile.gender}
        - Job: ${profile.job}
        - Relationship status: ${profile.relationshipStatus}
        - Fortune categories to cover: ${profile.fortuneCategories.joinToString { it.label }}

        [Saju Chart]
        - Day Master: ${profile.dayMaster}
        - Year Pillar: ${profile.yearPillar.describe()}
        - Month Pillar: ${profile.monthPillar.describe()}
        - Day Pillar: ${profile.dayPillar.describe()}
        - Hour Pillar: ${profile.hourPillar?.describe() ?: "unknown"}
        - Ohaeng (Five Elements) distribution (character count): ${profile.ohaeng.entries.joinToString { "${it.key} ${it.value}" }}
        - Sipseong (Ten Gods) distribution (character count): ${profile.sipseong.entries.joinToString { "${it.key} ${it.value}" }}

        [Year $year's Pillar (세운)]
        - $year: ${yearPillar.describe()}

        [Writing Rules]
        1. title: Headline copy for the year's fortune. 30 characters or fewer.
        2. content: Overall interpretation of the year's fortune. 500 characters or fewer. Base it on the relationship between the member's Saju chart and the year's pillar.
        3. score: integer between 0 and 100, an overall score for the year.
        4. fortuneCategories: RELATIONSHIP, LOVE, ACHIEVEMENT, MONEY, HEALTH — evaluate all 5, then return exactly the top 3 by star rating
           (ties broken by relevance to the member's Saju chart, then at random). Each entry must have a distinct category.
           - star: integer between 1 and 3.
        5. Write all sentences in Korean, keeping a warm and positive tone while avoiding unfounded exaggeration.
        """.trimIndent()

    private fun Pillar.describe(): String = formatSajuPillar(stem, branch, stemSipseong, branchSipseong, sibiunseong)
}
