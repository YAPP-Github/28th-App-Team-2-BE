package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.GetYearPillarPort
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.currentDate
import com.yapp.todakun.yearfortune.FortuneCategoryStar
import com.yapp.todakun.yearfortune.YearSelectionFortune
import com.yapp.todakun.yearfortune.port.inbound.CreateYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneResult
import com.yapp.todakun.yearfortune.port.outbound.GeneratedYearSelectionFortune
import com.yapp.todakun.yearfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.yearfortune.port.outbound.Pillar
import com.yapp.todakun.yearfortune.port.outbound.YearSelectionFortuneAiPort
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * AI 생성 입력 조립 → AI 호출 → YearSelectionFortune 저장까지 한 트랜잭션으로 처리한다.
 * [YearSelectionFortuneRepository.findByMemberIdAndYear] 선조회로 멱등성을 보장한다(이미 생성된 연도면 AI를 재호출하지 않는다).
 */
@CommandService
class CreateYearSelectionFortuneService(
    private val yearSelectionFortuneRepository: YearSelectionFortuneRepository,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getYearPillarPort: GetYearPillarPort,
    private val yearSelectionFortuneAiPort: YearSelectionFortuneAiPort,
) : CreateYearSelectionFortuneUseCase {
    @ExperimentalUuidApi
    override fun create(
        year: Int,
        memberId: UUID,
    ): YearSelectionFortuneResult {
        yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)?.let {
            return YearSelectionFortuneResult.from(it)
        }

        val generated = requestGeneration(memberId, year)

        val saved =
            yearSelectionFortuneRepository.save(
                YearSelectionFortune.create(
                    memberId = memberId,
                    year = year,
                    currentYear = currentDate().year,
                    score = generated.score,
                    title = generated.title,
                    content = generated.content,
                    fortuneCategories = generated.fortuneCategories.map { FortuneCategoryStar(it.fortuneCategory, it.star) },
                ),
            )

        return YearSelectionFortuneResult.from(saved)
    }

    private fun requestGeneration(
        memberId: UUID,
        year: Int,
    ): GeneratedYearSelectionFortune {
        val profile = buildProfile(memberId)
        val targetYearPillar = getYearPillarPort.getPillar(year).toPillar()

        return yearSelectionFortuneAiPort.generate(profile, year, targetYearPillar)
    }

    private fun buildProfile(memberId: UUID): MemberSajuProfile {
        val memberProfile = getMemberFortuneProfilePort.getProfile(memberId)
        val chart = getSajuChartPort.getChart(memberId)

        return MemberSajuProfile(
            birthDate = memberProfile.birthDate,
            gender = memberProfile.gender,
            job = memberProfile.job,
            relationshipStatus = memberProfile.relationshipStatus,
            favoriteFortuneCategories = memberProfile.favoriteFortuneCategories,
            dayMaster = chart.dayMaster,
            yearPillar = chart.yearPillar.toPillar(),
            monthPillar = chart.monthPillar.toPillar(),
            dayPillar = chart.dayPillar.toPillar(),
            hourPillar = chart.hourPillar?.toPillar(),
            ohaeng = chart.ohaeng,
            sipseong = chart.sipseong,
        )
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
