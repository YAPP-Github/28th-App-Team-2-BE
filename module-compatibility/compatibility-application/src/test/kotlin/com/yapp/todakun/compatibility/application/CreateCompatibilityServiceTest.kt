package com.yapp.todakun.compatibility.application

import com.yapp.todakun.compatibility.CompatibilityElement
import com.yapp.todakun.compatibility.CompatibilityOhaeng
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiPort
import com.yapp.todakun.compatibility.port.outbound.GeneratedCompatibility
import com.yapp.todakun.shared.CompatibilityChartView
import com.yapp.todakun.shared.CompatibilityChartsView
import com.yapp.todakun.shared.GetSajuChartsForCompatibilityPort
import com.yapp.todakun.shared.PillarSummary
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

@ExperimentalUuidApi
class CreateCompatibilityServiceTest :
    DescribeSpec({
        val getSajuChartsForCompatibilityPort = mockk<GetSajuChartsForCompatibilityPort>()
        val sajuCompatibilityTransactionalStore = mockk<SajuCompatibilityTransactionalStore>()
        val compatibilityAiPort = mockk<CompatibilityAiPort>()
        val service =
            CreateCompatibilityService(
                getSajuChartsForCompatibilityPort,
                sajuCompatibilityTransactionalStore,
                compatibilityAiPort,
            )

        val memberId = UUID.fromString("018f0000-0000-7000-8000-000000000001")
        val partnerLinkId = UUID.fromString("018f0000-0000-7000-8000-0000000000d2")
        val charts = compatibilityChartsView()

        afterTest { clearMocks(getSajuChartsForCompatibilityPort, sajuCompatibilityTransactionalStore, compatibilityAiPort) }

        beforeTest {
            every { getSajuChartsForCompatibilityPort.getCharts(memberId, partnerLinkId) } returns charts
        }

        describe("create") {
            context("이미 그 조합의 궁합이 있으면") {
                it("AI를 재호출하지 않고 저장된 궁합을 반환한다") {
                    val existing = sajuCompatibility(memberId, charts.myChartId, charts.partnerChartId)
                    every {
                        sajuCompatibilityTransactionalStore.findExistingWithLock(memberId, charts.myChartId, charts.partnerChartId)
                    } returns existing

                    val result = service.create(memberId, partnerLinkId)

                    result.id shouldBe existing.id
                    result.partnerName shouldBe charts.partnerName
                    verify(exactly = 0) { compatibilityAiPort.generate(any()) }
                    verify(exactly = 0) { sajuCompatibilityTransactionalStore.saveIfAbsent(any()) }
                }
            }

            context("아직 생성된 적이 없으면") {
                it("두 명식 오행 계산 + AI 총운으로 궁합을 저장하고 반환한다") {
                    every {
                        sajuCompatibilityTransactionalStore.findExistingWithLock(memberId, charts.myChartId, charts.partnerChartId)
                    } returns null
                    every { compatibilityAiPort.generate(any()) } returns generatedCompatibility()
                    val savedSlot = slot<SajuCompatibility>()
                    every { sajuCompatibilityTransactionalStore.saveIfAbsent(capture(savedSlot)) } answers { savedSlot.captured }

                    val result = service.create(memberId, partnerLinkId)

                    result.relationshipType shouldBe CompatibilityRelationshipType.LOVER
                    savedSlot.captured.ohaengs.sumOf { it.percentage } shouldBe 100
                    verify(exactly = 1) { sajuCompatibilityTransactionalStore.saveIfAbsent(any()) }
                }

                it("선조회 트랜잭션 → (트랜잭션 밖) AI 호출 → 저장 트랜잭션 순서로 처리한다") {
                    every {
                        sajuCompatibilityTransactionalStore.findExistingWithLock(memberId, charts.myChartId, charts.partnerChartId)
                    } returns null
                    every { compatibilityAiPort.generate(any()) } returns generatedCompatibility()
                    every { sajuCompatibilityTransactionalStore.saveIfAbsent(any()) } answers { firstArg() }

                    service.create(memberId, partnerLinkId)

                    verifyOrder {
                        sajuCompatibilityTransactionalStore.findExistingWithLock(memberId, charts.myChartId, charts.partnerChartId)
                        compatibilityAiPort.generate(any())
                        sajuCompatibilityTransactionalStore.saveIfAbsent(any())
                    }
                }
            }
        }
    })

private fun pillarSummary(): PillarSummary =
    PillarSummary(stem = "계", branch = "사", stemSipseong = "비견", branchSipseong = "정재", sibiunseong = "제왕")

private fun compatibilityChartView(ohaeng: Map<String, Int>): CompatibilityChartView =
    CompatibilityChartView(
        dayMaster = "계",
        yearPillar = pillarSummary(),
        monthPillar = pillarSummary(),
        dayPillar = pillarSummary(),
        hourPillar = null,
        ohaeng = ohaeng,
        sipseong = mapOf("비견" to 2, "정재" to 1),
    )

private fun compatibilityChartsView(): CompatibilityChartsView =
    CompatibilityChartsView(
        myChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c1"),
        partnerChartId = UUID.fromString("018f0000-0000-7000-8000-0000000000c2"),
        partnerName = "토실이",
        relationshipType = "LOVER",
        myChart = compatibilityChartView(mapOf("WATER" to 3, "FIRE" to 3, "METAL" to 1, "EARTH" to 1)),
        partnerChart = compatibilityChartView(mapOf("FIRE" to 4, "WOOD" to 2, "EARTH" to 2)),
    )

private fun generatedCompatibility(): GeneratedCompatibility =
    GeneratedCompatibility(
        score = 85,
        headline = "함께할수록 빛나는 궁합",
        subheadline = "함께 있을 때, 편안함이 커지는 사이예요.",
        summary = "두 분은 서로의 부족한 기운을 보완하며 평온한 안식처가 되어주는 최상의 흐름을 가지고 있습니다.",
        totalAnalysis = "따뜻한 기운과 유연한 기운이 만나 아름다운 관계를 이룹니다.",
    )

private fun sajuCompatibility(
    memberId: UUID,
    myChartId: UUID,
    partnerChartId: UUID,
): SajuCompatibility =
    SajuCompatibility.reconstitute(
        id = UUID.fromString("018f0000-0000-7000-8000-0000000000e1"),
        memberId = memberId,
        myChartId = myChartId,
        partnerChartId = partnerChartId,
        relationshipType = CompatibilityRelationshipType.LOVER,
        score = 85,
        headline = "함께할수록 빛나는 궁합",
        subheadline = "함께 있을 때, 편안함이 커지는 사이예요.",
        summary = "두 분은 서로의 부족한 기운을 보완합니다.",
        totalAnalysis = "총운 분석 내용",
        analysisBasis = "사주 팔자 기반",
        ohaengs = CompatibilityElement.entries.map { CompatibilityOhaeng(it, 20) },
    )
