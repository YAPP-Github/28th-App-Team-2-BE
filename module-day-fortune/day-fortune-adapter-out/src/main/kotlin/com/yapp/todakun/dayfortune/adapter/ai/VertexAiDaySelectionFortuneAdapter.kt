package com.yapp.todakun.dayfortune.adapter.ai

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneEmptyResponseException
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneGenerationFailedException
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.GeneratedDaySelectionFortune
import com.yapp.todakun.dayfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dayfortune.port.outbound.Pillar
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Vertex AI(Gemini)로 택일 운세를 생성하는 [DaySelectionFortuneAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedDaySelectionFortune]) 매핑을 전담한다.
 */
@Component
class VertexAiDaySelectionFortuneAdapter(
    chatClientBuilder: ChatClient.Builder,
) : DaySelectionFortuneAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(
        profile: MemberSajuProfile,
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        dayPillar: Pillar,
    ): GeneratedDaySelectionFortune {
        val generated =
            try {
                callAi(profile, purpose, targetDate, dayPillar)
            } catch (e: Exception) {
                throw DaySelectionFortuneGenerationFailedException(e)
            }

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

    private fun Pillar.describe(): String {
        val stemPart = stemSipseong?.let { "천간 $it, " } ?: ""

        return "$stem$branch (${stemPart}지지 $branchSipseong, 십이운성 $sibiunseong)"
    }
}
