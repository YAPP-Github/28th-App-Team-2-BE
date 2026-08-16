package com.yapp.todakun.dailyfortune.adapter.ai

import com.yapp.todakun.common.resilience.AiResilienceSupport
import com.yapp.todakun.dailyfortune.exception.DailyFortuneCircuitOpenException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneEmptyResponseException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.dailyfortune.exception.DailyFortuneTimeoutException
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dailyfortune.port.outbound.Pillar
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.concurrent.TimeoutException

private const val AI_RESILIENCE_INSTANCE_NAME = "daily-fortune-ai"

/**
 * Vertex AI(Gemini)로 오늘의 운세를 생성하는 [DailyFortuneAiPort] 구현체.
 * 프롬프트 구성과 구조화 출력(JSON → [GeneratedDailyFortune]) 매핑을 전담한다.
 * AI 호출은 [AiResilienceSupport]로 CircuitBreaker+TimeLimiter를 적용한다(Retry는 배치 Step이 이미 재시도하므로 미적용).
 */
@Component
class VertexAiDailyFortuneAdapter(
    chatClientBuilder: ChatClient.Builder,
    private val resilience: AiResilienceSupport,
) : DailyFortuneAiPort {
    private val chatClient = chatClientBuilder.build()

    override fun generate(
        profile: MemberSajuProfile,
        fortuneDate: LocalDate,
        todayPillar: Pillar,
    ): GeneratedDailyFortune {
        val generated =
            try {
                resilience.execute(AI_RESILIENCE_INSTANCE_NAME) { callAi(profile, fortuneDate, todayPillar) }
            } catch (e: CallNotPermittedException) {
                throw DailyFortuneCircuitOpenException(e)
            } catch (e: TimeoutException) {
                throw DailyFortuneTimeoutException(e)
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
        - Fortune categories to cover: ${profile.fortuneCategories.joinToString { it.label }}

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
           - title: a single concrete action the member can actually do today — not an abstract wish, mood, or outcome statement (avoid phrasing like "안정적인 수입으로 재물 운 상승"). One specific, doable behavior phrased with a Korean "~하기" noun ending. 30 characters or fewer.
             Example style per category: RELATIONSHIP "오랜만에 생각난 사람에게 메시지 보내기", LOVE "평소보다 밝은 컬러의 옷 착용하기", ACHIEVEMENT "미뤄둔 작은 업무 하나 먼저 끝내기", HEALTH "10분 정도 가볍게 산책하거나 스트레칭하기", MONEY "사용하지 않는 구독 서비스나 자동결제 내역 확인하기".
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
