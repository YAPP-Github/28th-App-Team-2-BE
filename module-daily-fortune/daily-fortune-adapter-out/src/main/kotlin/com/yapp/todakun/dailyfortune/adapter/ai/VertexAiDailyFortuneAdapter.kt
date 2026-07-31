package com.yapp.todakun.dailyfortune.adapter.ai

import com.yapp.todakun.dailyfortune.exception.DailyFortuneEmptyResponseException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dailyfortune.port.outbound.Pillar
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Vertex AI(Gemini)로 오늘의 운세를 생성하는 [DailyFortuneAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedDailyFortune]) 매핑을 전담한다.
 */
@Component
class VertexAiDailyFortuneAdapter(
    chatClientBuilder: ChatClient.Builder,
) : DailyFortuneAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(
        profile: MemberSajuProfile,
        fortuneDate: LocalDate,
        todayPillar: Pillar,
    ): GeneratedDailyFortune {
        val generated =
            try {
                callAi(profile, fortuneDate, todayPillar)
            } catch (e: Exception) {
                throw DailyFortuneGenerationFailedException(e)
            }

        return generated ?: throw DailyFortuneEmptyResponseException()
    }

    private fun callAi(
        profile: MemberSajuProfile,
        fortuneDate: LocalDate,
        todayPillar: Pillar,
    ): GeneratedDailyFortune? =
        chatClient
            .prompt()
            .user(buildPrompt(profile, fortuneDate, todayPillar))
            .call()
            .entity(GeneratedDailyFortune::class.java)

    private fun buildPrompt(
        profile: MemberSajuProfile,
        fortuneDate: LocalDate,
        todayPillar: Pillar,
    ): String =
        """
        You are a content writer producing a "today's fortune" reading based on traditional Korean Saju (Four Pillars) astrology.
        Using the member info below, write the fortune for $fortuneDate.

        [Member Info]
        - Birth date: ${profile.birthDate}
        - Gender: ${profile.gender}
        - Job: ${profile.job}
        - Relationship status: ${profile.relationshipStatus}
        - Favorite fortune categories: ${profile.favoriteFortuneCategories.joinToString { it.label }}

        [Saju Chart]
        - Day Master: ${profile.dayMaster}
        - Year Pillar: ${profile.yearPillar.describe()}
        - Month Pillar: ${profile.monthPillar.describe()}
        - Day Pillar: ${profile.dayPillar.describe()}
        - Hour Pillar: ${profile.hourPillar?.describe() ?: "unknown"}
        - Ohaeng (Five Elements) distribution (character count): ${profile.ohaeng.entries.joinToString { "${it.key} ${it.value}" }}
        - Sipseong (Ten Gods) distribution (character count): ${profile.sipseong.entries.joinToString { "${it.key} ${it.value}" }}

        [Today's Pillar]
        - $fortuneDate: ${todayPillar.describe()}

        [Writing Rules]
        1. title: Today's fortune headline copy. 30 characters or fewer.
        2. content: Overall interpretation of today's fortune. 800 characters or fewer. Base it on the relationship between the member's Saju chart and today's pillar.
        3. categoryFortunes: RELATIONSHIP, LOVE, ACHIEVEMENT, MONEY, HEALTH — include exactly one of each of the 5 categories.
           - score: integer between 0 and 100.
           - title: one-line action copy for today. 30 characters or fewer.
           - content: detailed interpretation. 200 characters or fewer.
        4. luckyItems: exactly 5 items. Each a concrete noun (color, object, etc.) of 8 characters or fewer.
           Choose items that compensate for the element(s) that are lacking in the member's Ohaeng distribution relative to today's pillar.
        5. cautionaryItems: exactly 5 items. Each a concrete noun of 8 characters or fewer.
           Choose items to avoid today that clash with today's pillar or aggravate an element already overrepresented in the member's Ohaeng distribution.
        6. Write all sentences in Korean, keeping a warm and positive tone while avoiding unfounded exaggeration.
        """.trimIndent()

    private fun Pillar.describe(): String {
        val stemPart = stemSipseong?.let { "천간 $it, " } ?: ""

        return "$stem$branch (${stemPart}지지 $branchSipseong, 십이운성 $sibiunseong)"
    }
}
