package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.SajuPillar
import com.yapp.todakun.saju.SajuRole
import com.yapp.todakun.saju.exception.SajuChartNotFoundException
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.CompatibilityChartView
import com.yapp.todakun.shared.CompatibilityChartsView
import com.yapp.todakun.shared.GetSajuChartsForCompatibilityPort
import com.yapp.todakun.shared.PillarSummary
import java.util.UUID

/**
 * 궁합 생성용 본인(SELF)·상대(PARTNER) 명식 조회 크로스 도메인 유스케이스([GetSajuChartsForCompatibilityPort] 구현).
 * 상대 링크가 회원 소유의 PARTNER 링크인지 검증하고, 두 명식을 축약 뷰로 변환한다.
 * 오행은 궁합 도메인이 자체 오행 enum으로 매핑할 수 있도록 코드([Element.name])를 key로 담는다.
 */
@QueryService
class GetSajuChartsForCompatibilityService(
    private val memberSajuLinkRepository: MemberSajuLinkRepository,
    private val sajuChartRepository: SajuChartRepository,
) : GetSajuChartsForCompatibilityPort {
    override fun getCharts(
        memberId: UUID,
        partnerLinkId: UUID,
    ): CompatibilityChartsView {
        val selfLink = memberSajuLinkRepository.findSelfByMemberId(memberId) ?: throw SajuChartNotFoundException()
        val partnerLink =
            memberSajuLinkRepository.findByIdAndMemberId(partnerLinkId, memberId)
                ?.takeIf { it.role == SajuRole.PARTNER }
                ?: throw SajuChartNotFoundException()

        val myChart = sajuChartRepository.findById(selfLink.chartId) ?: throw SajuChartNotFoundException()
        val partnerChart = sajuChartRepository.findById(partnerLink.chartId) ?: throw SajuChartNotFoundException()

        return CompatibilityChartsView(
            myChartId = myChart.id,
            partnerChartId = partnerChart.id,
            partnerName = partnerChart.name,
            relationshipType = (partnerLink.relationshipType ?: throw SajuChartNotFoundException()).name,
            myChart = myChart.toView(),
            partnerChart = partnerChart.toView(),
        )
    }

    private fun SajuChart.toView(): CompatibilityChartView =
        CompatibilityChartView(
            dayMaster = dayMaster.reading,
            yearPillar = pillars.summaryOf(PillarType.YEAR),
            monthPillar = pillars.summaryOf(PillarType.MONTH),
            dayPillar = pillars.summaryOf(PillarType.DAY),
            hourPillar = pillars.summaryOfOrNull(PillarType.HOUR),
            ohaeng = ohaeng.associate { it.element.name to it.count },
            sipseong = sipseong.associate { it.sipseong.label to it.count },
        )

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
