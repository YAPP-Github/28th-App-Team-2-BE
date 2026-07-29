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
    ): GeneratedDailyFortune =
        runCatching { callAi(profile, fortuneDate, todayPillar) }
            .getOrElse { throw DailyFortuneGenerationFailedException(it) }
            ?: throw DailyFortuneEmptyResponseException()

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
        당신은 한국 전통 사주 명리학에 기반해 "오늘의 운세"를 작성하는 콘텐츠 작가입니다.
        아래 회원 정보를 바탕으로 $fortuneDate 하루치 운세를 작성하세요.

        [회원 정보]
        - 생년월일: ${profile.birthDate}
        - 성별: ${profile.gender}
        - 직업: ${profile.job}
        - 연애 상태: ${profile.relationshipStatus}
        - 관심 있는 운세 카테고리: ${profile.favoriteFortuneCategories.joinToString { it.label }}

        [사주 명식]
        - 일간: ${profile.dayMaster}
        - 년주: ${profile.yearPillar.describe()}
        - 월주: ${profile.monthPillar.describe()}
        - 일주: ${profile.dayPillar.describe()}
        - 시주: ${profile.hourPillar?.describe() ?: "모름"}
        - 오행 분포(글자 수): ${profile.ohaeng.entries.joinToString { "${it.key} ${it.value}개" }}
        - 십성 분포(글자 수): ${profile.sipseong.entries.joinToString { "${it.key} ${it.value}개" }}

        [오늘의 일진]
        - $fortuneDate: ${todayPillar.describe()}

        [작성 규칙]
        1. title: 오늘의 운세 헤드라인 카피. 30자 이하.
        2. content: 오늘의 운세 종합 해석. 800자 이하. 회원의 사주 명식과 오늘 일진의 관계를 바탕으로 작성.
        3. categoryFortunes: RELATIONSHIP(관계운), LOVE(연애운), ACHIEVEMENT(성취운), MONEY(금전운), HEALTH(건강운)
           5개 카테고리를 정확히 1개씩 포함.
           - score: 0~100 사이 정수.
           - title: 오늘의 한 줄 액션 카피. 30자 이하.
           - content: 상세 해석. 200자 이하.
        4. luckyItems: 정확히 5개. 각각 8자 이하의 구체적인 명사(색상, 사물 등).
           회원의 오행 분포 중 오늘 일진 기준으로 부족한 오행을 보완하는 아이템으로 선정.
        5. cautionaryItems: 정확히 5개. 각각 8자 이하의 구체적인 명사.
           회원의 오행 분포 중 오늘 일진과 상충하거나 이미 과다한 오행을 자극하는, 오늘 피해야 할 아이템으로 선정.
        6. 모든 문장은 한국어로, 다정하고 긍정적인 톤을 유지하되 근거 없는 과장은 피하세요.
        """.trimIndent()

    private fun Pillar.describe(): String {
        val stemPart = stemSipseong?.let { "천간 $it, " } ?: ""

        return "$stem$branch (${stemPart}지지 $branchSipseong, 십이운성 $sibiunseong)"
    }
}
