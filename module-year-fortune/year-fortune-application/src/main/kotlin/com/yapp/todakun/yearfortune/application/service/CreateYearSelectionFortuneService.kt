package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.shared.FortuneCategory
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
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 연도별 운세 생성 오케스트레이터. 트랜잭션을 걸지 않는다 — 외부 AI 호출을 DB 트랜잭션 밖에서 수행하기 위함이다.
 * (선조회 → AI 생성 → 저장) 각 DB 단계는 [YearSelectionFortuneTransactionalStore]의 독립 트랜잭션으로 위임한다.
 * 1) [YearSelectionFortuneTransactionalStore.findExistingWithLock]로 락+선조회(멱등: 이미 생성된 연도면 AI를 재호출하지 않는다).
 * 2) 트랜잭션 밖에서 AI를 호출하고 도메인 엔티티를 조립(검증 포함)한다.
 * 3) [YearSelectionFortuneTransactionalStore.saveIfAbsent]로 락+재조회 후 멱등 저장한다(동시 요청 시 유니크 제약 충돌 방지).
 *
 * `create()` 자체는 트랜잭션을 걸지 않아 [YearSelectionFortuneTransactionalStore]의 각 단계가 이미 커밋된 뒤에야 반환되므로,
 * `@Cacheable`이 캐시에 적립하는 시점은 항상 커밋 이후다(이슈 #56). 생성된 연도별 운세 자체는 저장 후 불변이지만, 그 값은
 * [GetSajuChartPort]로 읽은 명식에 의존하므로 사주가 바뀌거나 삭제되면 [SajuChartChangedEventListener]가 해당 캐시를 함께 비운다.
 */
@Service
class CreateYearSelectionFortuneService(
    private val yearSelectionFortuneTransactionalStore: YearSelectionFortuneTransactionalStore,
    private val getMemberFortuneProfilePort: GetMemberFortuneProfilePort,
    private val getSajuChartPort: GetSajuChartPort,
    private val getYearPillarPort: GetYearPillarPort,
    private val yearSelectionFortuneAiPort: YearSelectionFortuneAiPort,
) : CreateYearSelectionFortuneUseCase {
    @Cacheable(cacheNames = [CacheNames.YEAR_FORTUNE], key = "#memberId + ':' + #year")
    @ExperimentalUuidApi
    override fun create(
        year: Int,
        memberId: UUID,
    ): YearSelectionFortuneResult {
        yearSelectionFortuneTransactionalStore.findExistingWithLock(memberId, year)?.let {
            return YearSelectionFortuneResult.from(it)
        }

        val generated = requestGeneration(memberId, year)
        val yearSelectionFortune =
            YearSelectionFortune.create(
                memberId = memberId,
                year = year,
                currentYear = currentDate().year,
                score = generated.score,
                title = generated.title,
                content = generated.content,
                fortuneCategories = generated.fortuneCategories.map { FortuneCategoryStar(it.fortuneCategory, it.star) },
            )

        return YearSelectionFortuneResult.from(yearSelectionFortuneTransactionalStore.saveIfAbsent(yearSelectionFortune))
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
