package com.yapp.todakun.config

import com.ninjasquad.springmockk.MockkBean
import com.yapp.todakun.common.cache.CacheNames
import com.yapp.todakun.saju.MemberSajuLink
import com.yapp.todakun.saju.SajuChart
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.inbound.GetMySajuUseCase
import com.yapp.todakun.saju.port.outbound.MemberSajuLinkRepository
import com.yapp.todakun.saju.port.outbound.SajuChartRepository
import com.yapp.todakun.shared.ReplaceSelfSajuChartPort
import com.yapp.todakun.terms.Terms
import com.yapp.todakun.terms.TermsType
import com.yapp.todakun.terms.port.inbound.GetTermsUseCase
import com.yapp.todakun.terms.repository.TermsRepository
import io.kotest.core.spec.style.DescribeSpec
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
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * `@Cacheable`/`@CacheEvict`가 실제 [RedisCacheConfig]·TestContainer Redis를 통해 동작하는지 검증한다(이슈 #56).
 * 서비스 단위 MockK 테스트(예: GetTermsServiceTest)는 `service.method(...)`를 직접 호출해 Spring AOP
 * 프록시를 타지 않으므로 캐시 적중·무효화 자체는 검증하지 못한다. 이 테스트는 UseCase를 Mock으로
 * 대체하지 않고 실제 프록시된 빈을 그대로 호출하고, 한 단계 아래 리포지토리만 MockK로 대체해 호출
 * 횟수로 캐시 동작을 검증한다.
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
    private val replaceSelfSajuChartPort: ReplaceSelfSajuChartPort,
    private val cacheManager: CacheManager,
) : DescribeSpec() {
    @MockkBean
    private lateinit var termsRepository: TermsRepository

    @MockkBean
    private lateinit var memberSajuLinkRepository: MemberSajuLinkRepository

    @MockkBean
    private lateinit var sajuChartRepository: SajuChartRepository

    init {
        afterTest { clearMocks(termsRepository, memberSajuLinkRepository, sajuChartRepository) }
        beforeTest {
            cacheManager.getCache(CacheNames.TERMS)?.clear()
            cacheManager.getCache(CacheNames.SAJU_CHART_DETAIL)?.clear()
        }

        describe("GetTermsService.getAllTerms (TTL 기반 캐시)") {
            context("같은 요청을 반복하면") {
                it("두 번째 호출부터는 리포지토리를 다시 조회하지 않는다") {
                    every { termsRepository.findAll() } returns
                        listOf(Terms.reconstitute(UUID.randomUUID(), TermsType.SERVICE, "서비스 이용약관", required = true))

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

                    verify(exactly = 2) { memberSajuLinkRepository.findSelfByMemberId(SajuFixture.MEMBER_ID) }
                }
            }
        }
    }
}
