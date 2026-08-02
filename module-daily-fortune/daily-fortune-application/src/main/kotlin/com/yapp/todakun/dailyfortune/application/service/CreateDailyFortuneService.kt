package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dailyfortune.port.outbound.Pillar
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.CreateLuckActionPort
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.PillarSummary
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 크로스 도메인 포트 [CreateDailyFortunePort] 구현체.
 * AI 생성 입력 조립 → AI 호출 → DailyFortune/LuckAction 저장까지 한 트랜잭션으로 처리한다.
 */
@CommandService
class CreateDailyFortuneService(
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getDailyPillarPort: GetDailyPillarPort,
    private val dailyFortuneAiPort: DailyFortuneAiPort,
    private val createLuckActionPort: CreateLuckActionPort,
) : CreateDailyFortunePort {
    @ExperimentalUuidApi
    override fun create(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): UUID {
        dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate)?.let { return it.id }

        val generated = requestGeneration(memberId, fortuneDate)
        validateCategoryFortunes(generated)

        val saved =
            dailyFortuneRepository.save(
                DailyFortune.create(
                    memberId = memberId,
                    fortuneDate = fortuneDate,
                    categoryScores = generated.categoryFortunes.map { it.score },
                    title = generated.title,
                    content = generated.content,
                    luckyItems = generated.luckyItems,
                    cautionaryItems = generated.cautionaryItems,
                ),
            )

        generated.categoryFortunes.forEach {
            createLuckActionPort.create(
                memberId = memberId,
                fortuneCategory = it.fortuneCategory,
                fortuneDate = fortuneDate,
                score = it.score,
                title = it.title,
                content = it.content,
            )
        }

        return saved.id
    }

    private fun requestGeneration(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): GeneratedDailyFortune {
        val profile = buildProfile(memberId)
        val todayPillar = getDailyPillarPort.getPillar(fortuneDate).toPillar()

        return dailyFortuneAiPort.generate(profile, fortuneDate, todayPillar)
    }

    private fun buildProfile(memberId: UUID): MemberSajuProfile {
        val memberProfile = getMemberFortuneProfilePort.getProfile(memberId)
        val chart = getSajuChartPort.getChart(memberId)

        return MemberSajuProfile(
            birthDate = memberProfile.birthDate,
            gender = memberProfile.gender,
            job = memberProfile.job,
            relationshipStatus = memberProfile.relationshipStatus,
            fortuneCategories = FortuneCategory.entries.toList(),
            dayMaster = chart.dayMaster,
            yearPillar = chart.yearPillar.toPillar(),
            monthPillar = chart.monthPillar.toPillar(),
            dayPillar = chart.dayPillar.toPillar(),
            hourPillar = chart.hourPillar?.toPillar(),
            ohaeng = chart.ohaeng,
            sipseong = chart.sipseong,
        )
    }

    /** AI가 5개 카테고리를 정확히 1개씩 채웠는지 검증한다(누락·중복 시 그대로 저장하면 LuckAction이 어긋난다). */
    private fun validateCategoryFortunes(generated: GeneratedDailyFortune) {
        val categories = generated.categoryFortunes.map { it.fortuneCategory }

        if (categories.size != FortuneCategory.entries.size || categories.toSet() != FortuneCategory.entries.toSet()) {
            throw DailyFortuneGenerationFailedException()
        }
    }

    private fun PillarSummary.toPillar(): Pillar =
        Pillar(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )
}
