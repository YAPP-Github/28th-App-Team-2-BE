package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.FortuneCategoryStar
import com.yapp.todakun.dayfortune.port.inbound.CreateDaySelectionFortuneUseCase
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dayfortune.port.outbound.Pillar
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.currentDate
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * AI 생성 입력 조립 → AI 호출 → DaySelectionFortune 저장까지, 후보 날짜 각각에 대해 한 트랜잭션으로 처리한다.
 * [DaySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate] 선조회로 날짜별 멱등성을 보장한다
 * (이미 생성된 (목적, 날짜) 조합이면 AI를 재호출하지 않는다 — 후보 날짜 중 일부만 재요청돼도 나머지만 새로 생성한다).
 * 동시 요청 대비 조회 전에 [DaySelectionFortuneRepository.lock]으로 (memberId, purpose, targetDate) 생성 구간을 직렬화한다
 * (락 없이는 두 요청이 동시에 조회 결과 null을 보고 각각 AI 호출·저장을 시도해 유니크 제약 충돌이 날 수 있다).
 */
@CommandService
class CreateDaySelectionFortuneService(
    private val daySelectionFortuneRepository: DaySelectionFortuneRepository,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getDailyPillarPort: GetDailyPillarPort,
    private val daySelectionFortuneAiPort: DaySelectionFortuneAiPort,
) : CreateDaySelectionFortuneUseCase {
    @ExperimentalUuidApi
    override fun create(
        purpose: DaySelectionPurpose,
        targetDates: List<LocalDate>,
        memberId: UUID,
    ): List<DaySelectionFortuneResult> {
        val profile = buildProfile(memberId)

        return targetDates.map { targetDate -> createOne(purpose, targetDate, memberId, profile) }
    }

    @ExperimentalUuidApi
    private fun createOne(
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        memberId: UUID,
        profile: MemberSajuProfile,
    ): DaySelectionFortuneResult {
        daySelectionFortuneRepository.lock(memberId, purpose, targetDate)

        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)?.let {
            return DaySelectionFortuneResult.from(it)
        }

        val dayPillar = getDailyPillarPort.getPillar(targetDate).toPillar()
        val generated = daySelectionFortuneAiPort.generate(profile, purpose, targetDate, dayPillar)

        val saved =
            daySelectionFortuneRepository.save(
                DaySelectionFortune.create(
                    memberId = memberId,
                    purpose = purpose,
                    targetDate = targetDate,
                    currentDate = currentDate(),
                    score = generated.score,
                    title = generated.title,
                    content = generated.content,
                    fortuneCategories = generated.fortuneCategories.map { FortuneCategoryStar(it.fortuneCategory, it.star) },
                ),
            )

        return DaySelectionFortuneResult.from(saved)
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
