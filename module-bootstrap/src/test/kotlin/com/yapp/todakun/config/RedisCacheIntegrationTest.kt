package com.yapp.todakun.config

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationInProgressException
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.port.inbound.GetTodayFortuneUseCase
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneStatus
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.luck.fixture.LuckActionFixture
import com.yapp.todakun.luck.port.inbound.GetLuckActionsUseCase
import com.yapp.todakun.luck.repository.LuckActionRepository
import com.yapp.todakun.member.BirthTime
import com.yapp.todakun.member.CalendarType
import com.yapp.todakun.member.Gender
import com.yapp.todakun.member.Job
import com.yapp.todakun.member.RelationshipStatus
import com.yapp.todakun.member.fixture.MemberFixture
import com.yapp.todakun.member.port.inbound.UpdateMemberCommand
import com.yapp.todakun.member.port.inbound.UpdateMemberUseCase
import com.yapp.todakun.member.repository.MemberRepository
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.inbound.GetMySajuUseCase
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.CreateDailyFortunePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.ReplaceSelfSajuChartPort
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.port.inbound.GetTermsUseCase
import com.yapp.todakun.terms.repository.TermsRepository
import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.port.inbound.CreateYearSelectionFortuneUseCase
import com.yapp.todakun.yearfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.yearfortune.port.outbound.GeneratedYearSelectionFortune
import com.yapp.todakun.yearfortune.port.outbound.YearSelectionFortuneAiPort
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * `@Cacheable`/`@CacheEvict`가 실제 [RedisCacheConfig]·TestContainer Redis를 통해 동작하는지 검증한다(이슈 #56).
 * 서비스 단위 MockK 테스트(예: GetTermsServiceTest)는 `service.method(...)`를 직접 호출해 Spring AOP
 * 프록시를 타지 않으므로 캐시 적중·무효화 자체는 검증하지 못한다. 이 테스트는 UseCase를 Mock으로
 * 대체하지 않고 실제 프록시된 빈을 그대로 호출하고, 한 단계 아래 리포지토리만 MockK로 대체해 호출
 * 횟수로 캐시 동작을 검증한다.
 *
 * 6종의 캐시(TERMS·SAJU_CHART_DETAIL·SAJU_CHART_SUMMARY·TODAY_FORTUNE·LUCK_ACTIONS·YEAR_FORTUNE)를 모두 왕복 직렬화/역직렬화까지 검증한다.
 * `FailOpenCacheErrorHandler`가 캐시 조작 실패를 흡수(fail-open)하는 구조라,
 * 값 하나라도 직렬화·역직렬화가 깨지면 예외 없이 조용히 항상 캐시 미스로만 동작해 "캐시가 적용됐다"고
 * 믿는 채로 실제로는 매번 DB를 다시 타는 상태가 될 수 있다(이슈 #56 P1-3). 리포지토리 호출 횟수가 1회로
 * 유지되는지가 이 왕복이 실제로 성공했다는 증거다.
 *
 * Redis TestContainer는 로컬에서 `withReuse(true)`로 재사용되어(테스트 스킬 참고) 이전 실행에서
 * 남은 캐시 엔트리가 다음 실행에 영향을 줄 수 있다. 각 `it` 시작 전 대상 캐시를 명시적으로 비워
 * 실행 순서·이전 실행 상태와 무관하게 독립적으로 동작하도록 한다.
 */
@ExperimentalUuidApi
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(TestContainersConfig::class, DailyFortuneAiMockConfig::class)
class RedisCacheIntegrationTest(
    private val getTermsUseCase: GetTermsUseCase,
    private val getMySajuUseCase: GetMySajuUseCase,
    private val getSajuChartPort: GetSajuChartPort,
    private val getTodayFortuneUseCase: GetTodayFortuneUseCase,
    private val getLuckActionsUseCase: GetLuckActionsUseCase,
    private val createYearSelectionFortuneUseCase: CreateYearSelectionFortuneUseCase,
    private val replaceSelfSajuChartPort: ReplaceSelfSajuChartPort,
    private val updateMemberUseCase: UpdateMemberUseCase,
    private val yearSelectionFortuneAiPort: YearSelectionFortuneAiPort,
    private val cacheManager: CacheManager,
) : DescribeSpec() {
    @MockkBean
    private lateinit var termsRepository: TermsRepository

    @MockkBean
    private lateinit var memberSajuLinkRepository: MemberSajuLinkRepository

    @MockkBean
    private lateinit var sajuChartRepository: SajuChartRepository

    @MockkBean
    private lateinit var dailyFortuneRepository: DailyFortuneRepository

    @MockkBean
    private lateinit var luckActionRepository: LuckActionRepository

    @MockkBean
    private lateinit var yearSelectionFortuneRepository: YearSelectionFortuneRepository

    @MockkBean
    private lateinit var memberRepository: MemberRepository

    @MockkBean
    private lateinit var createDailyFortunePort: CreateDailyFortunePort

    init {
        afterTest {
            clearMocks(
                termsRepository,
                memberSajuLinkRepository,
                sajuChartRepository,
                dailyFortuneRepository,
                luckActionRepository,
                yearSelectionFortuneRepository,
                memberRepository,
                createDailyFortunePort,
            )
        }
        beforeTest {
            cacheManager.getCache(CacheNames.TERMS)?.clear()
            cacheManager.getCache(CacheNames.SAJU_CHART_DETAIL)?.clear()
            cacheManager.getCache(CacheNames.SAJU_CHART_SUMMARY)?.clear()
            cacheManager.getCache(CacheNames.TODAY_FORTUNE)?.clear()
            cacheManager.getCache(CacheNames.LUCK_ACTIONS)?.clear()
            cacheManager.getCache(CacheNames.YEAR_FORTUNE)?.clear()
        }

        describe("GetTermsService.getAllTerms (TTL 기반 캐시)") {
            context("같은 요청을 반복하면") {
                it("두 번째 호출부터는 리포지토리를 다시 조회하지 않는다") {
                    every { termsRepository.findAll() } returns
                        listOf(
                            Terms.reconstitute(
                                Uuid.generateV7().toJavaUuid(),
                                TermsType.SERVICE,
                                "서비스 이용약관",
                                required = true,
                            ),
                        )

                    getTermsUseCase.getAllTerms()
                    getTermsUseCase.getAllTerms()

                    verify(exactly = 1) { termsRepository.findAll() }
                }
            }
        }

        describe("GetMySajuService.getMine (TTL 기반 캐시)") {
            context("같은 회원을 반복 조회하면") {
                it("두 번째 호출부터는 리포지토리를 다시 조회하지 않는다") {
                    val chart = SajuFixture.chart()
                    val link = SajuFixture.selfLink(chartId = chart.id)
                    every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns link
                    every { sajuChartRepository.findById(chart.id) } returns chart

                    getMySajuUseCase.getMine(SajuFixture.MEMBER_ID)
                    getMySajuUseCase.getMine(SajuFixture.MEMBER_ID)

                    verify(exactly = 1) { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) }
                }
            }
        }

        describe("GetSajuChartService.getChart (TTL 기반 캐시)") {
            context("같은 회원을 반복 조회하면") {
                it("두 번째 호출부터는 리포지토리를 다시 조회하지 않는다") {
                    val chart = SajuFixture.chart()
                    val link = SajuFixture.selfLink(chartId = chart.id)
                    every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns link
                    every { sajuChartRepository.findById(chart.id) } returns chart

                    getSajuChartPort.getChart(SajuFixture.MEMBER_ID)
                    getSajuChartPort.getChart(SajuFixture.MEMBER_ID)

                    verify(exactly = 1) { sajuChartRepository.findById(chart.id) }
                }
            }
        }

        describe("GetTodayFortuneService.getToday (TTL 기반 캐시)") {
            context("같은 회원·날짜를 반복 조회하면") {
                it("두 번째 호출부터는 리포지토리를 다시 조회하지 않는다") {
                    val dailyFortune = DailyFortuneFixture.create()
                    val luckAction = LuckActionFixture.create(fortuneDate = dailyFortune.fortuneDate)
                    every {
                        dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)
                    } returns dailyFortune
                    every {
                        luckActionRepository.findAllByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)
                    } returns listOf(luckAction)

                    getTodayFortuneUseCase.getToday(dailyFortune.memberId, dailyFortune.fortuneDate)
                    getTodayFortuneUseCase.getToday(dailyFortune.memberId, dailyFortune.fortuneDate)

                    verify(exactly = 1) {
                        dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)
                    }
                }
            }

            context("생성 중(GENERATING) 결과를 반환하면") {
                it("캐시하지 않아 생성 완료 후 재조회 시 최신 결과를 돌려준다") {
                    val memberId = Uuid.generateV7().toJavaUuid()
                    val fortuneDate = LocalDate.now()
                    every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, any()) } returns null
                    every { createDailyFortunePort.create(memberId, fortuneDate) } throws DailyFortuneGenerationInProgressException()

                    val generatingResult = getTodayFortuneUseCase.getToday(memberId, fortuneDate)

                    generatingResult.status shouldBe TodayFortuneStatus.GENERATING

                    val dailyFortune = DailyFortuneFixture.create(memberId = memberId, fortuneDate = fortuneDate)
                    val luckAction = LuckActionFixture.create(memberId = memberId, fortuneDate = fortuneDate)
                    every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate) } returns dailyFortune
                    every {
                        luckActionRepository.findAllByMemberIdAndFortuneDate(memberId, fortuneDate)
                    } returns listOf(luckAction)

                    val completedResult = getTodayFortuneUseCase.getToday(memberId, fortuneDate)

                    completedResult.status shouldBe TodayFortuneStatus.COMPLETED
                }
            }
        }

        describe("GetLuckActionsService.getLuckActions (TTL 기반 캐시)") {
            context("같은 회원·날짜를 반복 조회하면") {
                it("두 번째 호출부터는 리포지토리를 다시 조회하지 않는다") {
                    val luckAction = LuckActionFixture.create()
                    every {
                        luckActionRepository.findAllByMemberIdAndFortuneDate(luckAction.memberId, luckAction.fortuneDate)
                    } returns listOf(luckAction)

                    getLuckActionsUseCase.getLuckActions(luckAction.memberId, luckAction.fortuneDate)
                    getLuckActionsUseCase.getLuckActions(luckAction.memberId, luckAction.fortuneDate)

                    verify(exactly = 1) {
                        luckActionRepository.findAllByMemberIdAndFortuneDate(luckAction.memberId, luckAction.fortuneDate)
                    }
                }
            }
        }

        describe("CreateYearSelectionFortuneService.create (TTL 기반 캐시)") {
            context("같은 회원·연도를 반복 생성 요청하면") {
                it("두 번째 호출부터는 AI를 다시 호출하지 않는다") {
                    val fortune = YearSelectionFortuneFixture.create()
                    val chart = SajuFixture.chart()
                    val link = SajuFixture.selfLink(chartId = chart.id)
                    every { memberSajuLinkRepository.findSelfByMemberId(fortune.memberId) } returns link
                    every { sajuChartRepository.findById(chart.id) } returns chart
                    every { memberRepository.findById(fortune.memberId) } returns MemberFixture.member(id = fortune.memberId)
                    every { yearSelectionFortuneRepository.lock(fortune.memberId, fortune.year) } just Runs
                    every { yearSelectionFortuneRepository.findByMemberIdAndYear(fortune.memberId, fortune.year) } returns null
                    every { yearSelectionFortuneRepository.save(any()) } answers { firstArg() }
                    every { yearSelectionFortuneAiPort.generate(any(), fortune.year, any()) } returns
                        GeneratedYearSelectionFortune(
                            title = fortune.title,
                            content = fortune.content,
                            score = fortune.score,
                            fortuneCategories = fortune.fortuneCategories.map { GeneratedCategoryFortune(it.fortuneCategory, it.star) },
                        )

                    createYearSelectionFortuneUseCase.create(fortune.year, fortune.memberId)
                    createYearSelectionFortuneUseCase.create(fortune.year, fortune.memberId)

                    verify(exactly = 1) { yearSelectionFortuneAiPort.generate(any(), fortune.year, any()) }
                }
            }
        }

        describe("ReplaceSelfSajuChartService.replace (evict 기반 캐시)") {
            context("캐시된 상태에서 명식이 재계산되면") {
                it("다음 조회는 캐시가 아니라 리포지토리에서 새로 읽어온다") {
                    val initialChart = SajuFixture.chart()
                    val initialLink = SajuFixture.selfLink(chartId = initialChart.id)
                    every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns initialLink
                    every { sajuChartRepository.findById(initialChart.id) } returns initialChart

                    getMySajuUseCase.getMine(SajuFixture.MEMBER_ID) // 캐시 적립

                    val savedChart = slot<SajuChart>()
                    val savedLink = slot<MemberSajuLink>()
                    every { sajuChartRepository.save(capture(savedChart)) } answers { savedChart.captured }
                    every { sajuChartRepository.deleteById(initialChart.id) } just Runs
                    every { memberSajuLinkRepository.save(capture(savedLink)) } answers { savedLink.captured }

                    // 실제 ManseryeokPort 어댑터(달력 연산, 외부 호출 없음)를 그대로 태워 명식을 재계산한다.
                    replaceSelfSajuChartPort.replace(
                        memberId = SajuFixture.MEMBER_ID,
                        name = "토닥이",
                        gender = "FEMALE",
                        calendarType = "SOLAR",
                        birthDate = LocalDate.of(2001, 5, 30),
                        birthTime = "MISI",
                        isLeapMonth = false,
                    )

                    every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns savedLink.captured
                    every { sajuChartRepository.findById(savedChart.captured.id) } returns savedChart.captured

                    getMySajuUseCase.getMine(SajuFixture.MEMBER_ID) // evict 됐으니 리포지토리에서 다시 읽어야 한다

                    // 캐시 미스 2회(캐시 적립·재조회) + ReplaceSelfSajuChartService.replace() 내부의 기존 링크 조회 1회 = 3회.
                    verify(exactly = 3) { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) }
                }
            }

            context("UpdateMemberService.update()처럼 중첩 트랜잭션 안에서 호출되면") {
                it("바깥 트랜잭션 커밋 후에도 캐시가 비워져 다음 조회는 리포지토리에서 새로 읽어온다") {
                    val initialChart = SajuFixture.chart()
                    val initialLink = SajuFixture.selfLink(chartId = initialChart.id)
                    every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns initialLink
                    every { sajuChartRepository.findById(initialChart.id) } returns initialChart

                    getMySajuUseCase.getMine(SajuFixture.MEMBER_ID) // 캐시 적립

                    every { memberRepository.findById(SajuFixture.MEMBER_ID) } returns MemberFixture.member(id = SajuFixture.MEMBER_ID)
                    every { memberRepository.save(any()) } answers { firstArg() }

                    val savedChart = slot<SajuChart>()
                    val savedLink = slot<MemberSajuLink>()
                    every { sajuChartRepository.save(capture(savedChart)) } answers { savedChart.captured }
                    every { sajuChartRepository.deleteById(initialChart.id) } just Runs
                    every { memberSajuLinkRepository.save(capture(savedLink)) } answers { savedLink.captured }

                    // UpdateMemberService.update()가 최외곽 트랜잭션이고, 그 안에서 ReplaceSelfSajuChartService.replace()가
                    // 중첩 트랜잭션(REQUIRED)으로 호출된다. replace()에 @CacheEvict를 직접 붙였다면 안쪽 트랜잭션이
                    // 물리 커밋 없이 반환한 직후 evict가 실행돼, update()가 실제로 커밋되기 전에 캐시가 비워지는
                    // 문제가 있었다(이슈 #56). 지금은 SajuChartChangedEvent + AFTER_COMMIT 리스너로 처리한다.
                    updateMemberUseCase.update(
                        UpdateMemberCommand(
                            memberId = SajuFixture.MEMBER_ID,
                            gender = Gender.FEMALE,
                            calendarType = CalendarType.SOLAR,
                            birthDate = LocalDate.of(2001, 5, 30),
                            birthTime = BirthTime.MISI,
                            job = Job.WORKER,
                            relationshipStatus = RelationshipStatus.DATING,
                        ),
                    )

                    every { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) } returns savedLink.captured
                    every { sajuChartRepository.findById(savedChart.captured.id) } returns savedChart.captured

                    getMySajuUseCase.getMine(SajuFixture.MEMBER_ID) // evict 됐으니 리포지토리에서 다시 읽어야 한다

                    // 캐시 미스 2회(캐시 적립·재조회) + ReplaceSelfSajuChartService.replace() 내부의 기존 링크 조회 1회 = 3회.
                    verify(exactly = 3) { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) }
                }
            }
        }
    }
}
