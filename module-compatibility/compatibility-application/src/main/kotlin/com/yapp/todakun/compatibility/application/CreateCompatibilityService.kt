package com.yapp.todakun.compatibility.application

import com.yapp.todakun.compatibility.CompatibilityOhaengCalculator
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.inbound.CompatibilityResult
import com.yapp.todakun.compatibility.port.inbound.CreateCompatibilityUseCase
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiInput
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiPort
import com.yapp.todakun.compatibility.port.outbound.CompatibilityChartProfile
import com.yapp.todakun.compatibility.port.outbound.CompatibilityPillar
import com.yapp.todakun.shared.CompatibilityChartView
import com.yapp.todakun.shared.CompatibilityChartsView
import com.yapp.todakun.shared.GetSajuChartsForCompatibilityPort
import com.yapp.todakun.shared.PillarSummary
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * 궁합 생성 오케스트레이터. 트랜잭션을 걸지 않는다 — 외부 AI 호출을 DB 트랜잭션 밖에서 수행하기 위함이다.
 * (선조회 → 오행 결정적 계산 + AI 총운 생성 → 저장) 각 DB 단계는 [SajuCompatibilityTransactionalStore]의 독립 트랜잭션으로 위임한다.
 * 1) [SajuCompatibilityTransactionalStore.findExistingWithLock]로 락+선조회(멱등: 이미 생성된 조합이면 AI를 재호출하지 않는다).
 * 2) 트랜잭션 밖에서 오행 계산과 AI 총운 생성을 수행하고 도메인 엔티티를 조립한다.
 * 3) [SajuCompatibilityTransactionalStore.saveIfAbsent]로 락+재조회 후 멱등 저장한다(동시 요청 시 유니크 제약 충돌 방지).
 */
@Service
class CreateCompatibilityService(
    private val getSajuChartsForCompatibilityPort: GetSajuChartsForCompatibilityPort,
    private val sajuCompatibilityTransactionalStore: SajuCompatibilityTransactionalStore,
    private val compatibilityAiPort: CompatibilityAiPort,
) : CreateCompatibilityUseCase {
    @ExperimentalUuidApi
    override fun create(
        memberId: UUID,
        partnerLinkId: UUID,
    ): CompatibilityResult {
        val charts = getSajuChartsForCompatibilityPort.getCharts(memberId, partnerLinkId)

        sajuCompatibilityTransactionalStore.findExistingWithLock(memberId, charts.myChartId, charts.partnerChartId)?.let {
            return CompatibilityResult.from(it, charts.partnerName)
        }

        val relationshipType = CompatibilityRelationshipType.from(charts.relationshipType)
        val ohaengs = CompatibilityOhaengCalculator.combine(charts.myChart.ohaeng, charts.partnerChart.ohaeng)
        val generated = compatibilityAiPort.generate(buildAiInput(relationshipType, charts))

        val sajuCompatibility =
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
            )

        return CompatibilityResult.from(
            sajuCompatibilityTransactionalStore.saveIfAbsent(sajuCompatibility),
            charts.partnerName,
        )
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
