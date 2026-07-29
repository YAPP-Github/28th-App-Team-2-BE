package com.yapp.todakun.compatibility.application

import com.yapp.todakun.common.annotation.CommandService
import com.yapp.todakun.compatibility.CompatibilityOhaengCalculator
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.inbound.CompatibilityResult
import com.yapp.todakun.compatibility.port.inbound.CreateCompatibilityUseCase
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiInput
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiPort
import com.yapp.todakun.compatibility.port.outbound.CompatibilityChartProfile
import com.yapp.todakun.compatibility.port.outbound.CompatibilityPillar
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import com.yapp.todakun.shared.CompatibilityChartView
import com.yapp.todakun.shared.CompatibilityChartsView
import com.yapp.todakun.shared.GetSajuChartsForCompatibilityPort
import com.yapp.todakun.shared.PillarSummary
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 궁합 생성 유스케이스. 두 명식 조회 → (멱등) 기존 궁합 선조회 → 오행 결정적 계산 + AI 총운 생성 → 저장을 한 트랜잭션으로 처리한다.
 * [SajuCompatibilityRepository.findByMemberIdAndCharts] 선조회로 멱등성을 보장한다(이미 생성된 조합이면 AI를 재호출하지 않는다).
 * 조회 전에 [SajuCompatibilityRepository.lock]으로 (내 명식, 상대 명식) 생성 구간을 직렬화해 동시 요청 시 유니크 제약 충돌을 막는다.
 */
@CommandService
class CreateCompatibilityService(
    private val getSajuChartsForCompatibilityPort: GetSajuChartsForCompatibilityPort,
    private val sajuCompatibilityRepository: SajuCompatibilityRepository,
    private val compatibilityAiPort: CompatibilityAiPort,
) : CreateCompatibilityUseCase {
    @ExperimentalUuidApi
    override fun create(
        memberId: UUID,
        partnerLinkId: UUID,
    ): CompatibilityResult {
        val charts = getSajuChartsForCompatibilityPort.getCharts(memberId, partnerLinkId)

        sajuCompatibilityRepository.lock(charts.myChartId, charts.partnerChartId)

        sajuCompatibilityRepository.findByMemberIdAndCharts(memberId, charts.myChartId, charts.partnerChartId)?.let {
            return CompatibilityResult.from(it, charts.partnerName)
        }

        val relationshipType = CompatibilityRelationshipType.from(charts.relationshipType)
        val ohaengs = CompatibilityOhaengCalculator.combine(charts.myChart.ohaeng, charts.partnerChart.ohaeng)
        val generated = compatibilityAiPort.generate(buildAiInput(relationshipType, charts))

        val saved =
            sajuCompatibilityRepository.save(
                SajuCompatibility.create(
                    memberId = memberId,
                    myChartId = charts.myChartId,
                    partnerChartId = charts.partnerChartId,
                    relationshipType = relationshipType,
                    score = generated.score,
                    headline = generated.headline,
                    subheadline = generated.subheadline,
                    summary = generated.summary,
                    totalAnalysis = generated.totalAnalysis,
                    ohaengs = ohaengs,
                ),
            )

        return CompatibilityResult.from(saved, charts.partnerName)
    }

    private fun buildAiInput(
        relationshipType: CompatibilityRelationshipType,
        charts: CompatibilityChartsView,
    ): CompatibilityAiInput =
        CompatibilityAiInput(
            relationshipType = relationshipType,
            myProfile = charts.myChart.toProfile(),
            partnerProfile = charts.partnerChart.toProfile(),
        )

    private fun CompatibilityChartView.toProfile(): CompatibilityChartProfile =
        CompatibilityChartProfile(
            dayMaster = dayMaster,
            yearPillar = yearPillar.toPillar(),
            monthPillar = monthPillar.toPillar(),
            dayPillar = dayPillar.toPillar(),
            hourPillar = hourPillar?.toPillar(),
            ohaeng = ohaeng,
            sipseong = sipseong,
        )

    private fun PillarSummary.toPillar(): CompatibilityPillar =
        CompatibilityPillar(
            stem = stem,
            branch = branch,
            stemSipseong = stemSipseong,
            branchSipseong = branchSipseong,
            sibiunseong = sibiunseong,
        )
}
