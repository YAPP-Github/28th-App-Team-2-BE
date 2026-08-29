package com.yapp.todakun.dayfortune.adapter.ai

import com.yapp.todakun.common.ai.vertexResponseSchema
import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneCircuitOpenException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneEmptyResponseException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneGenerationFailedException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneTimeoutException
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.GeneratedDaySelectionFortune
import com.yapp.todakun.dayfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dayfortune.port.outbound.Pillar
import com.yapp.todakun.shared.formatSajuPillar
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions
import org.springframework.stereotype.Component
import java.time.LocalDate

private const val AI_RESILIENCE_INSTANCE_NAME = "day-fortune-ai"

// 프롬프트 지시문만으로는 JSON 형식·구조가 강제되지 않아, Gemini가 문법적으로 깨진 JSON을 응답하거나 [GeneratedDaySelectionFortune]와 다른 구조(필드 누락, 타입 불일치)로 응답할 수 있다.
// provider 단에서 JSON 출력 모드 + entity() 변환 대상과 동일한 스키마를 강제해 BeanOutputConverter 파싱 실패를 줄인다.
// responseSchema는 vertexResponseSchema로 대문자 type을 올려 전달해야 Vertex Schema proto가 타입 정보를 실제로 인식한다(소문자는 TYPE_UNSPECIFIED로 무시됨).
private val JSON_RESPONSE_OPTIONS =
    VertexAiGeminiChatOptions.builder()
        .responseMimeType("application/json")
        .responseSchema(vertexResponseSchema(GeneratedDaySelectionFortune::class.java))
        .build()

/**
 * Vertex AI(Gemini)로 택일 운세를 생성하는 [DaySelectionFortuneAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedDaySelectionFortune]) 매핑을 전담한다.
 * AI 호출은 [AiResilienceSupport]로 CircuitBreaker+Retry+TimeLimiter를 적용한다.
 */
@Component
class VertexAiDaySelectionFortuneAdapter(
    chatClientBuilder: ChatClient.Builder,
    private val resilience: AiResilienceSupport,
) : DaySelectionFortuneAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(
        profile: MemberSajuProfile,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        dayPillar: Pillar,
    ): GeneratedDaySelectionFortune {
        val generated =
            resilience.execute(
                AI_RESILIENCE_INSTANCE_NAME,
                onCircuitOpen = { DaySelectionFortuneCircuitOpenException(it) },
                onTimeout = { DaySelectionFortuneTimeoutException(it) },
                onFailure = { DaySelectionFortuneGenerationFailedException(it) },
            ) { callAi(profile, purpose, targetDate, dayPillar) }

        return generated ?: throw DaySelectionFortuneEmptyResponseException()
    }

    private fun callAi(
        profile: MemberSajuProfile,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        dayPillar: Pillar,
    ): GeneratedDaySelectionFortune? =
        chatClient
            .prompt()
            .user(buildPrompt(profile, purpose, targetDate, dayPillar))
            .options(JSON_RESPONSE_OPTIONS)
            .call()
            .entity(GeneratedDaySelectionFortune::class.java)

    private fun buildPrompt(
        profile: MemberSajuProfile,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        dayPillar: Pillar,
    ): String =
        """
        You are a content writer producing a "day selection fortune" (택일 운세) reading based on traditional Korean Saju (Four Pillars) astrology.
        The member is considering the date $targetDate for the purpose of "${purpose.label}" (${purpose.name}).
        Using the member info below, judge how favorable this date is for that specific purpose.

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

        [Candidate Date $targetDate's Pillar (일진)]
        - $targetDate: ${dayPillar.describe()}

        [Writing Rules]
        1. title: Headline copy summarizing how favorable $targetDate is for "${purpose.label}". 30 characters or fewer.
        2. content: Overall interpretation of why the date suits (or doesn't suit) the purpose. 500 characters or fewer.
           Base it on the relationship between the member's Saju chart, the date's pillar (일진), and the stated purpose.
        3. score: integer between 0 and 100, an overall favorability score of the date for the stated purpose.
        4. fortuneCategories: RELATIONSHIP, LOVE, ACHIEVEMENT, MONEY, HEALTH — evaluate all 5, then return exactly the top 3 by star rating
           (ties broken by relevance to the member's Saju chart, then at random). Each entry must have a distinct category.
           - star: integer between 1 and 3.
        5. Write all sentences in Korean, keeping a warm and positive tone while avoiding unfounded exaggeration.
        """.trimIndent()

    private fun Pillar.describe(): String = formatSajuPillar(stem, branch, stemSipseong, branchSipseong, sibiunseong)
}
