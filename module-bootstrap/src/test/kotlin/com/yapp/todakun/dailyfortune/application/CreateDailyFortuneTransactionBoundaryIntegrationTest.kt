package com.yapp.todakun.dailyfortune.application

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.config.TransactionBoundaryProbe
import com.yapp.todakun.config.TransactionBoundarySnapshot
import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationInProgressException
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000101")
private val LUCK_ACTION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000102")

// 시나리오 간 영속 데이터가 섞이지 않도록 멱등성 검증은 별도 회원으로 수행한다(선언 순서에 의존하지 않게).
private val IDEMPOTENT_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000103")

// 동시 생성 경합 검증도 다른 시나리오와 겹치지 않도록 별도 회원으로 수행한다.
private val CONCURRENT_MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000104")

private val FORTUNE_DATE = LocalDate.of(2026, 8, 9)

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
                    stubCollaborators(MEMBER_ID)

                    lateinit var snapshot: TransactionBoundarySnapshot
                    every { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) } answers {
                        snapshot = probe.capture()
                        GENERATED_FORTUNE
                    }

                    val fortuneId = createDailyFortunePort.create(MEMBER_ID, FORTUNE_DATE)

                    snapshot.transactionActive shouldBe false
                    snapshot.entityManagerBound shouldBe false
                    verify(exactly = 1) { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) }

                    // 위 단언이 공허하지 않음을 보장하는 대조군 — 프로브는 트랜잭션 안에서는 점유를 실제로 감지한다.
                    val insideTransaction = transactionTemplate.execute<TransactionBoundarySnapshot> { probe.capture() }
                    insideTransaction?.transactionActive shouldBe true
                    insideTransaction?.entityManagerBound shouldBe true

                    val persisted =
                        transactionTemplate.execute<DailyFortune?> {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(MEMBER_ID, FORTUNE_DATE)
                        }
                    persisted?.id shouldBe fortuneId
                }
            }

            context("이미 생성된 조합이면") {
                it("AI를 재호출하지 않고 기존 결과를 반환한다") {
                    stubCollaborators(IDEMPOTENT_MEMBER_ID)
                    every { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) } returns GENERATED_FORTUNE

                    val firstId = createDailyFortunePort.create(IDEMPOTENT_MEMBER_ID, FORTUNE_DATE)
                    verify(exactly = 1) { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) }

                    val secondId = createDailyFortunePort.create(IDEMPOTENT_MEMBER_ID, FORTUNE_DATE)

                    secondId shouldBe firstId
                    // 두 번째 호출에서 누적 호출 수가 늘지 않는다 = 선조회로 끝났다.
                    verify(exactly = 1) { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) }
                }
            }

            // 이슈 #90: 가입 직후 백그라운드 리스너와 홈 화면 자가 치유(GetTodayFortuneService)가 같은 (memberId, fortuneDate)로
            // 동시에 들어와도, findExistingWithLock의 락은 AI 호출 전에 반납되므로 그 자체로는 중복 호출을 막지 못한다.
            // DailyFortuneGenerationLockPort가 이 경합을 막는지 실제 동시 호출로 검증한다.
            context("같은 (memberId, fortuneDate)로 두 호출이 동시에 들어오면") {
                it("AI는 한 번만 호출되고, 나머지 호출은 DailyFortuneGenerationInProgressException을 받는다") {
                    stubCollaborators(CONCURRENT_MEMBER_ID)
                    val aiCallStarted = CountDownLatch(1)
                    val releaseAiCall = CountDownLatch(1)
                    every { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) } answers {
                        aiCallStarted.countDown()
                        releaseAiCall.await(5, TimeUnit.SECONDS)
                        GENERATED_FORTUNE
                    }

                    val winnerResult = CompletableFuture.supplyAsync { createDailyFortunePort.create(CONCURRENT_MEMBER_ID, FORTUNE_DATE) }
                    aiCallStarted.await(5, TimeUnit.SECONDS) shouldBe true

                    val loserException =
                        shouldThrow<DailyFortuneGenerationInProgressException> {
                            createDailyFortunePort.create(CONCURRENT_MEMBER_ID, FORTUNE_DATE)
                        }

                    releaseAiCall.countDown()
                    loserException.shouldNotBeNull()
                    winnerResult.get(5, TimeUnit.SECONDS).shouldNotBeNull()
                    verify(exactly = 1) { dailyFortuneAiPort.generate(any(), FORTUNE_DATE, any()) }
                }
            }
        }
    }

    private fun stubCollaborators(memberId: UUID) {
        every { getMemberFortuneProfilePort.getProfile(memberId) } returns MEMBER_PROFILE
        every { getSajuChartPort.getChart(memberId) } returns SAJU_CHART
        every { getDailyPillarPort.getPillar(FORTUNE_DATE) } returns PILLAR
        every { createLuckActionPort.create(any(), any(), any(), any(), any(), any()) } returns LUCK_ACTION_ID
    }
}
