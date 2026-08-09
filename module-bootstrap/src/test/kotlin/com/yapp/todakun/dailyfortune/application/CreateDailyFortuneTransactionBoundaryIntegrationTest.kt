package com.yapp.todakun.dailyfortune.application

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.config.TransactionBoundarySnapshot
import com.yapp.todakun.config.captureTransactionBoundarySnapshot
import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.CreateLuckActionPort
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000101")
private val FORTUNE_DATE = LocalDate.of(2026, 8, 9)
private val LUCK_ACTION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000102")

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
    GeneratedDailyFortune(
        title = "활기찬 하루",
        content = "오늘은 좋은 하루입니다.",
        luckyItems = listOf("노란색", "마스크", "운동화", "셔츠", "안경"),
        cautionaryItems = listOf("검정색", "체크무늬", "라면", "시계", "우산"),
        categoryFortunes =
            FortuneCategory.entries.map {
                GeneratedCategoryFortune(fortuneCategory = it, score = 80, title = "제목", content = "내용")
            },
    )

/**
 * 이슈 #59: 오늘의 운세 생성 시 AI 호출 구간에 활성 트랜잭션·DB 커넥션 점유가 없음을 실제 Postgres로 검증한다.
 * [GetMemberFortuneProfilePort]·[GetSajuChartPort]·[GetDailyPillarPort]·[CreateLuckActionPort]는 다른 도메인과의
 * 크로스 도메인 확장점이라 목으로 대체하고, [DailyFortuneTransactionalStore]가 소유한 실제 JPA·트랜잭션 경로만 검증 대상으로 남긴다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class CreateDailyFortuneTransactionBoundaryIntegrationTest(
    private val createDailyFortunePort: CreateDailyFortunePort,
    private val dailyFortuneAiPort: DailyFortuneAiPort,
    private val dailyFortuneRepository: DailyFortuneRepository,
    private val dataSource: DataSource,
    transactionManager: PlatformTransactionManager,
) : DescribeSpec() {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    @MockkBean
    private lateinit var getMemberFortuneProfilePort: GetMemberFortuneProfilePort

    @MockkBean
    private lateinit var getSajuChartPort: GetSajuChartPort

    @MockkBean
    private lateinit var getDailyPillarPort: GetDailyPillarPort

    @MockkBean
    private lateinit var createLuckActionPort: CreateLuckActionPort

    init {
        afterTest {
            clearMocks(getMemberFortuneProfilePort, getSajuChartPort, getDailyPillarPort, createLuckActionPort, dailyFortuneAiPort)
        }

        describe("오늘의 운세 생성") {
            context("AI 생성이 필요한 새 (memberId, fortuneDate) 조합이면") {
                it("AI 호출 시점에 활성 트랜잭션도 DB 커넥션 점유도 없다") {
                    every { getMemberFortuneProfilePort.getProfile(MEMBER_ID) } returns MEMBER_PROFILE
                    every { getSajuChartPort.getChart(MEMBER_ID) } returns SAJU_CHART
                    every { getDailyPillarPort.getPillar(FORTUNE_DATE) } returns PILLAR
                    every { createLuckActionPort.create(any(), any(), any(), any(), any(), any()) } returns LUCK_ACTION_ID

                    lateinit var snapshot: TransactionBoundarySnapshot
                    every { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) } answers {
                        snapshot = dataSource.captureTransactionBoundarySnapshot()
                        GENERATED_FORTUNE
                    }

                    val fortuneId = createDailyFortunePort.create(MEMBER_ID, FORTUNE_DATE)

                    snapshot.transactionActive shouldBe false
                    snapshot.activeConnections shouldBe 0
                    val persisted =
                        transactionTemplate.execute<DailyFortune?> {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, FORTUNE_DATE)
                        }
                    persisted?.id shouldBe fortuneId
                }
            }

            context("이미 생성된 조합이면") {
                it("AI를 재호출하지 않고 기존 결과를 반환한다") {
                    every { getMemberFortuneProfilePort.getProfile(MEMBER_ID) } returns MEMBER_PROFILE
                    every { getSajuChartPort.getChart(MEMBER_ID) } returns SAJU_CHART
                    every { getDailyPillarPort.getPillar(FORTUNE_DATE) } returns PILLAR
                    every { createLuckActionPort.create(any(), any(), any(), any(), any(), any()) } returns LUCK_ACTION_ID
                    every { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) } returns GENERATED_FORTUNE

                    val firstId = createDailyFortunePort.create(MEMBER_ID, FORTUNE_DATE)
                    clearMocks(dailyFortuneAiPort, answers = false, recordedCalls = true)

                    val secondId = createDailyFortunePort.create(MEMBER_ID, FORTUNE_DATE)

                    secondId shouldBe firstId
                    verify(exactly = 0) { dailyFortuneAiPort.generate(any(), any(), any()) }
                }
            }
        }
    }
}
