package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.SajuPillar
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import org.springframework.cache.annotation.Cacheable
import java.util.UUID

/** 회원 본인(SELF) 사주 명식 축약 뷰 조회 크로스 도메인 유스케이스([GetSajuChartPort] 구현). */
@QueryService
class GetSajuChartService(
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val sajuChartRepository: SajuChartRepository,
) : GetSajuChartPort {
    // GetMySajuService와 같은 memberId를 키로 쓰지만 반환 타입이 달라 별도 캐시 이름을 쓴다(이슈 #56).
    @Cacheable(cacheNames = [CacheNames.SAJU_CHART_SUMMARY], key = "#memberId")
    override fun getChart(memberId: UUID): SajuChartSummary {
        val link = memberSajuLinkRepository.findSelfByMemberId(memberId) ?: throw SajuChartNotFoundException()
        val chart = sajuChartRepository.findById(link.chartId) ?: throw SajuChartNotFoundException()

        return SajuChartSummary(
            dayMaster = chart.dayMaster.reading,
            yearPillar = chart.pillars.summaryOf(PillarType.YEAR),
            monthPillar = chart.pillars.summaryOf(PillarType.MONTH),
            dayPillar = chart.pillars.summaryOf(PillarType.DAY),
            hourPillar = chart.pillars.summaryOfOrNull(PillarType.HOUR),
            ohaeng = chart.ohaeng.associate { it.element.label to it.count },
            sipseong = chart.sipseong.associate { it.sipseong.label to it.count },
        )
    }

    private fun List<SajuPillar>.summaryOf(pillarType: PillarType): PillarSummary = first { it.pillarType == pillarType }.toSummary()

    private fun List<SajuPillar>.summaryOfOrNull(pillarType: PillarType): PillarSummary? =
        firstOrNull { it.pillarType == pillarType }?.toSummary()

    private fun SajuPillar.toSummary(): PillarSummary =
        PillarSummary(
            stem = stem.reading,
            branch = branch.reading,
            stemSipseong = stemSipseong?.label,
            branchSipseong = branchSipseong.label,
            sibiunseong = sibiunseong.label,
        )
}
