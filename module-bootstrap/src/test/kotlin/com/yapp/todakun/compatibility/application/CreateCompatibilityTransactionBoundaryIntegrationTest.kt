package com.yapp.todakun.compatibility.application

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.compatibility.CompatibilityRelationshipType
import com.yapp.todakun.compatibility.SajuCompatibility
import com.yapp.todakun.compatibility.port.inbound.CreateCompatibilityUseCase
import com.yapp.todakun.compatibility.port.outbound.CompatibilityAiPort
import com.yapp.todakun.compatibility.port.outbound.GeneratedCompatibility
import com.yapp.todakun.compatibility.port.outbound.SajuCompatibilityRepository
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.config.TransactionBoundarySnapshot
import com.yapp.todakun.config.captureTransactionBoundarySnapshot
import com.yapp.todakun.shared.CompatibilityChartView
import com.yapp.todakun.shared.CompatibilityChartsView
import com.yapp.todakun.shared.GetSajuChartsForCompatibilityPort
import com.yapp.todakun.shared.PillarSummary
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import javax.sql.DataSource

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000401")
private val PARTNER_LINK_ID = UUID.fromString("018f0000-0000-7000-8000-000000000402")
private val MY_CHART_ID = UUID.fromString("018f0000-0000-7000-8000-000000000403")
private val PARTNER_CHART_ID = UUID.fromString("018f0000-0000-7000-8000-000000000404")

private val PILLAR = PillarSummary(stem = "갑", branch = "자", stemSipseong = "비견", branchSipseong = "정인", sibiunseong = "제왕")
private val CHART_VIEW =
    CompatibilityChartView(
        dayMaster = "갑목",
        yearPillar = PILLAR,
        monthPillar = PILLAR,
        dayPillar = PILLAR,
        hourPillar = null,
        ohaeng = mapOf("WOOD" to 2),
        sipseong = mapOf("비견" to 2),
    )
private val CHARTS_VIEW =
    CompatibilityChartsView(
        myChartId = MY_CHART_ID,
        partnerChartId = PARTNER_CHART_ID,
        partnerName = "김철수",
        relationshipType = CompatibilityRelationshipType.LOVER.name,
        myChart = CHART_VIEW,
        partnerChart = CHART_VIEW,
    )
private val GENERATED_COMPATIBILITY =
    GeneratedCompatibility(
        score = 80,
        headline = "천생연분",
        subheadline = "서로를 이해하는 사이",
        summary = "잘 맞는 궁합입니다.",
        totalAnalysis = "전반적으로 조화로운 관계입니다.",
    )

/**
 * 이슈 #59: 궁합 생성 시 AI 호출 구간에 활성 트랜잭션·DB 커넥션 점유가 없음을 실제 Postgres로 검증한다.
 * [GetSajuChartsForCompatibilityPort]는 크로스 도메인 확장점이라 목으로 대체하고,
 * [SajuCompatibilityTransactionalStore]가 소유한 실제 JPA·트랜잭션 경로만 검증 대상으로 남긴다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class CreateCompatibilityTransactionBoundaryIntegrationTest(
    private val createCompatibilityUseCase: CreateCompatibilityUseCase,
    private val compatibilityAiPort: CompatibilityAiPort,
    private val sajuCompatibilityRepository: SajuCompatibilityRepository,
    private val dataSource: DataSource,
    transactionManager: PlatformTransactionManager,
) : DescribeSpec() {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @MockkBean
    private lateinit var getSajuChartsForCompatibilityPort: GetSajuChartsForCompatibilityPort

    init {
        afterTest { clearMocks(getSajuChartsForCompatibilityPort, compatibilityAiPort) }

        describe("궁합 생성") {
            context("AI 생성이 필요한 새 (memberId, myChartId, partnerChartId) 조합이면") {
                it("AI 호출 시점에 활성 트랜잭션도 DB 커넥션 점유도 없다") {
                    every { getSajuChartsForCompatibilityPort.getCharts(MEMBER_ID, PARTNER_LINK_ID) } returns CHARTS_VIEW

                    lateinit var snapshot: TransactionBoundarySnapshot
                    every { compatibilityAiPort.generate(any()) } answers {
                        snapshot = dataSource.captureTransactionBoundarySnapshot()
                        GENERATED_COMPATIBILITY
                    }

                    val result = createCompatibilityUseCase.create(MEMBER_ID, PARTNER_LINK_ID)

                    snapshot.transactionActive shouldBe false
                    snapshot.activeConnections shouldBe 0
                    val persisted =
                        transactionTemplate.execute<SajuCompatibility?> {
                            sajuCompatibilityRepository.findByMemberIdAndCharts(MEMBER_ID, MY_CHART_ID, PARTNER_CHART_ID)
                        }
                    persisted?.id shouldBe result.id
                }
            }

            context("이미 생성된 조합이면") {
                it("AI를 재호출하지 않고 기존 결과를 반환한다") {
                    every { getSajuChartsForCompatibilityPort.getCharts(MEMBER_ID, PARTNER_LINK_ID) } returns CHARTS_VIEW
                    every { compatibilityAiPort.generate(any()) } returns GENERATED_COMPATIBILITY

                    val first = createCompatibilityUseCase.create(MEMBER_ID, PARTNER_LINK_ID)
                    clearMocks(compatibilityAiPort, answers = false, recordedCalls = true)

                    val second = createCompatibilityUseCase.create(MEMBER_ID, PARTNER_LINK_ID)

                    second.id shouldBe first.id
                    verify(exactly = 0) { compatibilityAiPort.generate(any()) }
                }
            }
        }
    }
}
