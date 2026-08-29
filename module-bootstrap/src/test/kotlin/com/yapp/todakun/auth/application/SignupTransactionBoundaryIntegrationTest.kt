package com.yapp.todakun.auth.application

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.auth.OauthMemberProfile
import com.yapp.todakun.auth.application.service.SignupTransactionService
import com.yapp.todakun.auth.port.inbound.SignupCommand
import com.yapp.todakun.config.DailyFortuneAiMockConfig
import com.yapp.todakun.config.TestContainersConfig
import com.yapp.todakun.dailyfortune.DailyFortune
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.CreateLuckActionPort
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.OauthProvider
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

private val LUCK_ACTION_ID = UUID.fromString("018f0000-0000-7000-8000-000000000201")

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
private val SIGNUP_COMMAND =
    SignupCommand(
        onboardingToken = "unused-onboarding-token",
        name = "홍길동",
        birthDate = LocalDate.of(2000, 1, 1),
        birthTime = "MYOSI",
        calendarType = "SOLAR",
        gender = "MALE",
        job = "STUDENT",
        relationshipStatus = "SOLO",
    )

/**
 * 이슈 #90: 회원가입 트랜잭션 커밋 이후에만 [com.yapp.todakun.shared.event.MemberSignedUpEvent] 리스너가 실행되고,
 * 실제 AI 호출은 요청 스레드가 아닌 전용 워커 스레드에서 일어남을 실제 Postgres 트랜잭션으로 검증한다.
 * [GetMemberFortuneProfilePort]·[GetSajuChartPort]·[GetDailyPillarPort]·[CreateLuckActionPort]는 daily-fortune 입장에서
 * 다른 도메인과의 크로스 도메인 확장점이라 목으로 대체하고([com.yapp.todakun.dailyfortune.application.CreateDailyFortuneTransactionBoundaryIntegrationTest]와 동일),
 * [SignupTransactionService]가 소유한 실제 회원가입 트랜잭션 경계와 이벤트 리스너 배선만 검증 대상으로 남긴다.
 */
@ExperimentalUuidApi
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class SignupTransactionBoundaryIntegrationTest(
    private val signupTransactionService: SignupTransactionService,
    private val dailyFortuneAiPort: DailyFortuneAiPort,
    private val dailyFortuneRepository: DailyFortuneRepository,
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

        describe("회원가입 트랜잭션과 오늘의 운세 생성 이벤트") {
            context("register() 트랜잭션이 롤백되면") {
                it("MemberSignedUpEvent 리스너가 실행되지 않아 AI를 호출하지 않는다") {
                    val profile = newOauthProfile(providerId = "rollback-${Uuid.generateV7().toJavaUuid()}")
                    val command = SIGNUP_COMMAND

                    transactionTemplate.execute { status ->
                        signupTransactionService.register(profile, command)
                        status.setRollbackOnly()
                    }

                    // AFTER_COMMIT 리스너는 물리 커밋 시에만 스케줄되므로, 롤백이면 비동기 대기 없이도 결정적으로 호출되지 않는다.
                    verify(exactly = 0) { dailyFortuneAiPort.generate(any(), any(), any()) }
                }
            }

            context("register() 트랜잭션이 커밋되면") {
                it("요청 스레드가 아닌 전용 워커 스레드에서 오늘의 운세를 생성한다") {
                    val profile = newOauthProfile(providerId = "commit-${Uuid.generateV7().toJavaUuid()}")
                    val command = SIGNUP_COMMAND
                    stubCollaborators()
                    val generationThreadName = AtomicReference<String>()
                    val generatedFortuneDate = AtomicReference<LocalDate>()
                    // 리스너는 비동기 워커 스레드에서 currentDate()를 다시 계산하므로, 테스트 초기화 시점 날짜와 자정을 사이에 두고 어긋날 수 있다.
                    // 실제 호출에 쓰인 날짜를 캡처해 이후 스텁·조회에 동일하게 재사용한다.
                    every { dailyFortuneAiPort.generate(any(), any(), any()) } answers {
                        generatedFortuneDate.set(secondArg())
                        generationThreadName.set(Thread.currentThread().name)
                        GENERATED_FORTUNE
                    }
                    val requestThreadName = Thread.currentThread().name

                    val memberId = signupTransactionService.register(profile, command)

                    eventually(5.seconds) {
                        generationThreadName.get().shouldNotBeNull()
                        persistedFortune(memberId, generatedFortuneDate.get()).shouldNotBeNull()
                    }
                    generationThreadName.get() shouldStartWith "signup-fortune-"
                    generationThreadName.get() shouldNotBe requestThreadName
                    persistedFortune(memberId, generatedFortuneDate.get())?.title shouldBe GENERATED_FORTUNE.title
                }
            }
        }
    }

    private fun newOauthProfile(providerId: String): OauthMemberProfile =
        OauthMemberProfile(provider = OauthProvider.KAKAO, providerId = providerId, email = "$providerId@todakun.com")

    // OSIV가 꺼져 있어 트랜잭션 밖에서는 리포지토리를 호출할 수 없다.
    private fun persistedFortune(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): DailyFortune? =
        transactionTemplate.execute {
            dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate)
        }

    private fun stubCollaborators() {
        every { getMemberFortuneProfilePort.getProfile(any()) } returns MEMBER_PROFILE
        every { getSajuChartPort.getChart(any()) } returns SAJU_CHART
        every { getDailyPillarPort.getPillar(any()) } returns PILLAR
        every { createLuckActionPort.create(any(), any(), any(), any(), any(), any()) } returns LUCK_ACTION_ID
    }
}
