package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.FortuneCategoryStar
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dayfortune.port.outbound.Pillar
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.currentDate
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * (memberId, purpose, targetDate) 한 건을 독립 트랜잭션으로 생성한다 — 날짜별로 커밋을 분리해
 * 락/커넥션 점유 시간을 줄이고, 한 날짜의 실패가 이미 커밋된 다른 날짜를 롤백시키지 않게 한다.
 * [DaySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate] 선조회로 멱등성을 보장한다
 * (이미 생성된 조합이면 회원 프로필/사주 조회와 AI 호출을 건너뛰고 그대로 재사용한다).
 * 조회 전에 [DaySelectionFortuneRepository.lock]으로 (memberId, purpose, targetDate) 생성 구간을 직렬화한다
 * (락 없이는 두 요청이 동시에 조회 결과 null을 보고 각각 AI 호출·저장을 시도해 유니크 제약 충돌이 날 수 있다).
 */
@CommandService
class CreateOneDaySelectionFortuneService(
    private val daySelectionFortuneRepository: DaySelectionFortuneRepository,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getDailyPillarPort: GetDailyPillarPort,
    private val daySelectionFortuneAiPort: DaySelectionFortuneAiPort,
) {
    @ExperimentalUuidApi
    fun createOne(
        purpose: DaySelectionPurpose,
        targetDate: LocalDate,
        memberId: UUID,
    ): DaySelectionFortuneResult {
        daySelectionFortuneRepository.lock(memberId, purpose, targetDate)

        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)?.let {
            return DaySelectionFortuneResult.from(it)
        }

        val profile = buildProfile(memberId)
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

    private fun PillarSummary.toPillar(): Pillar =
        Pillar(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )
}
