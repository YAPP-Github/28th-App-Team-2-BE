package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.FortuneCategoryStar
import com.yapp.todakun.dayfortune.port.inbound.DaySelectionFortuneResult
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.MemberSajuProfile
import com.yapp.todakun.dayfortune.port.outbound.Pillar
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.currentDate
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * (memberId, purpose, targetDate) 한 건 생성을 오케스트레이션한다. 트랜잭션을 걸지 않는다 — 외부 AI 호출을 DB 트랜잭션 밖에서 수행하기 위함이다.
 * (선조회 → AI 생성 → 저장) 각 DB 단계는 [DaySelectionFortuneTransactionalStore]의 독립 트랜잭션으로 위임하므로,
 * 날짜별 커밋이 분리되고(한 날짜의 실패가 다른 날짜를 롤백시키지 않음) 락/커넥션 점유 시간이 짧게 유지된다.
 * 1) [DaySelectionFortuneTransactionalStore.findExistingWithLock]로 락+선조회(멱등: 이미 생성된 조합이면 회원 프로필/사주 조회와 AI 호출을 건너뛴다).
 * 2) 트랜잭션 밖에서 AI를 호출하고 도메인 엔티티를 조립(검증 포함)한다.
 * 3) [DaySelectionFortuneTransactionalStore.saveIfAbsent]로 락+재조회 후 멱등 저장한다(동시 요청 시 유니크 제약 충돌 방지).
 */
@Service
class CreateOneDaySelectionFortuneService(
    private val daySelectionFortuneTransactionalStore: DaySelectionFortuneTransactionalStore,
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
        daySelectionFortuneTransactionalStore.findExistingWithLock(memberId, purpose, targetDate)?.let {
            return DaySelectionFortuneResult.from(it)
        }

        val profile = buildProfile(memberId)
        val dayPillar = getDailyPillarPort.getPillar(targetDate).toPillar()
        val generated = daySelectionFortuneAiPort.generate(profile, purpose, targetDate, dayPillar)

        val daySelectionFortune =
            DaySelectionFortune.create(
                memberId = memberId,
                purpose = purpose,
                targetDate = targetDate,
                currentDate = currentDate(),
                score = generated.score,
                title = generated.title,
                content = generated.content,
                fortuneCategories = generated.fortuneCategories.map { FortuneCategoryStar(it.fortuneCategory, it.star) },
            )

        return DaySelectionFortuneResult.from(daySelectionFortuneTransactionalStore.saveIfAbsent(daySelectionFortune))
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
