package com.yapp.todakun.yearfortune.application

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.config.TransactionBoundaryProbe
import com.yapp.todakun.config.TransactionBoundarySnapshot
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.GetYearPillarPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import com.yapp.todakun.shared.currentDate
import com.yapp.todakun.yearfortune.YearSelectionFortune
import com.yapp.todakun.yearfortune.port.inbound.CreateYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.yearfortune.port.outbound.GeneratedYearSelectionFortune
import com.yapp.todakun.yearfortune.port.outbound.YearSelectionFortuneAiPort
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000201")

// 시나리오 간 영속 데이터가 섞이지 않도록 멱등성 검증은 별도 회원으로 수행한다(선언 순서에 의존하지 않게).
private val IDEMPOTENT_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000202")

private val YEAR = currentDate().year

private val MEMBER_PROFILE =
    MemberFortuneProfile(
        name = "홍길동",
        birthDate = LocalDate.of(1996, 3, 2),
        gender = "MALE",
        job = "회사원",
        relationshipStatus = "SINGLE",
    )
private val PILLAR = PillarSummary(stem = "갑", branch = "자", stemSipseong = "비견", branchSipseong = "정인", sibiunseong = "제왕")
private val SAJU_CHART =
    SajuChartSummary(
        dayMaster = "갑목",
        yearPillar = PILLAR,
        monthPillar = PILLAR,
        dayPillar = PILLAR,
        hourPillar = null,
        ohaeng = mapOf("WOOD" to 2),
        sipseong = mapOf("비견" to 2),
    )
private val GENERATED_FORTUNE =
    GeneratedYearSelectionFortune(
        title = "도약의 해",
        content = "새로운 기회가 찾아오는 한 해입니다.",
        score = 80,
        fortuneCategories =
            listOf(
                GeneratedCategoryFortune(fortuneCategory = FortuneCategory.MONEY, star = 3),
                GeneratedCategoryFortune(fortuneCategory = FortuneCategory.ACHIEVEMENT, star = 2),
                GeneratedCategoryFortune(fortuneCategory = FortuneCategory.HEALTH, star = 1),
            ),
    )

/**
 * 이슈 #59: 연도별 운세 생성 시 AI 호출 구간에 활성 트랜잭션·DB 커넥션 점유가 없음을 실제 Postgres로 검증한다.
 * [GetMemberFortuneProfilePort]·[GetSajuChartPort]·[GetYearPillarPort]는 크로스 도메인 확장점이라 목으로 대체하고,
 * [YearSelectionFortuneTransactionalStore]가 소유한 실제 JPA·트랜잭션 경로만 검증 대상으로 남긴다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class CreateYearSelectionFortuneTransactionBoundaryIntegrationTest(
    private val createYearSelectionFortuneUseCase: CreateYearSelectionFortuneUseCase,
    private val yearSelectionFortuneAiPort: YearSelectionFortuneAiPort,
    private val yearSelectionFortuneRepository: YearSelectionFortuneRepository,
    dataSource: DataSource,
    entityManagerFactory: EntityManagerFactory,
    transactionManager: PlatformTransactionManager,
) : DescribeSpec() {
    private val probe = TransactionBoundaryProbe(dataSource, entityManagerFactory)
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @MockkBean
    private lateinit var getMemberFortuneProfilePort: GetMemberFortuneProfilePort

    @MockkBean
    private lateinit var getSajuChartPort: GetSajuChartPort

    @MockkBean
    private lateinit var getYearPillarPort: GetYearPillarPort

    init {
        afterTest { clearMocks(getMemberFortuneProfilePort, getSajuChartPort, getYearPillarPort, yearSelectionFortuneAiPort) }

        describe("연도별 운세 생성") {
            context("AI 생성이 필요한 새 (memberId, year) 조합이면") {
                it("AI 호출 시점에 활성 트랜잭션도 DB 커넥션 점유도 없다") {
                    stubCollaborators(MEMBER_ID)

                    lateinit var snapshot: TransactionBoundarySnapshot
                    every { yearSelectionFortuneAiPort.generate(any(), YEAR, any()) } answers {
                        snapshot = probe.capture()
                        GENERATED_FORTUNE
                    }

                    val result = createYearSelectionFortuneUseCase.create(YEAR, MEMBER_ID)

                    snapshot.transactionActive shouldBe false
                    snapshot.entityManagerBound shouldBe false
                    verify(exactly = 1) { yearSelectionFortuneAiPort.generate(any(), YEAR, any()) }

                    // 위 단언이 공허하지 않음을 보장하는 대조군 — 프로브는 트랜잭션 안에서는 점유를 실제로 감지한다.
                    val insideTransaction = transactionTemplate.execute<TransactionBoundarySnapshot> { probe.capture() }
                    insideTransaction?.transactionActive shouldBe true
                    insideTransaction?.entityManagerBound shouldBe true

                    val persisted =
                        transactionTemplate.execute<YearSelectionFortune?> {
                            yearSelectionFortuneRepository.findByMemberIdAndYear(MEMBER_ID, YEAR)
                        }
                    persisted?.id shouldBe result.id
                }
            }

            context("이미 생성된 조합이면") {
                it("AI를 재호출하지 않고 기존 결과를 반환한다") {
                    stubCollaborators(IDEMPOTENT_MEMBER_ID)
                    every { yearSelectionFortuneAiPort.generate(any(), YEAR, any()) } returns GENERATED_FORTUNE

                    val first = createYearSelectionFortuneUseCase.create(YEAR, IDEMPOTENT_MEMBER_ID)
                    verify(exactly = 1) { yearSelectionFortuneAiPort.generate(any(), YEAR, any()) }

                    val second = createYearSelectionFortuneUseCase.create(YEAR, IDEMPOTENT_MEMBER_ID)

                    second.id shouldBe first.id
                    // 두 번째 호출에서 누적 호출 수가 늘지 않는다 = 선조회로 끝났다.
                    verify(exactly = 1) { yearSelectionFortuneAiPort.generate(any(), YEAR, any()) }
                }
            }
        }
    }

    private fun stubCollaborators(memberId: UUID) {
        every { getMemberFortuneProfilePort.getProfile(memberId) } returns MEMBER_PROFILE
        every { getSajuChartPort.getChart(memberId) } returns SAJU_CHART
        every { getYearPillarPort.getPillar(YEAR) } returns PILLAR
    }
}
