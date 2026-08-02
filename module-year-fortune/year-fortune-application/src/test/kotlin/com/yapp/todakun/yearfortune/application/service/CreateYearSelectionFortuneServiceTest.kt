package com.yapp.todakun.yearfortune.application.service

import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.GetYearPillarPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
import com.yapp.todakun.yearfortune.exception.YearSelectionFortuneCategoryDuplicatedException
import com.yapp.todakun.yearfortune.fixture.YearSelectionFortuneFixture
import com.yapp.todakun.yearfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.yearfortune.port.outbound.GeneratedYearSelectionFortune
import com.yapp.todakun.yearfortune.port.outbound.YearSelectionFortuneAiPort
import com.yapp.todakun.yearfortune.repository.YearSelectionFortuneRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class CreateYearSelectionFortuneServiceTest :
    DescribeSpec({
        val yearSelectionFortuneRepository = mockk<YearSelectionFortuneRepository>()
        val getMemberFortuneProfilePort = mockk<GetMemberFortuneProfilePort>()
        val getSajuChartPort = mockk<GetSajuChartPort>()
        val getYearPillarPort = mockk<GetYearPillarPort>()
        val yearSelectionFortuneAiPort = mockk<YearSelectionFortuneAiPort>()
        val service =
            CreateYearSelectionFortuneService(
                yearSelectionFortuneRepository,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getYearPillarPort,
                yearSelectionFortuneAiPort,
            )

        val memberId = Uuid.generateV7().toJavaUuid()
        val year = 2026

        afterTest {
            clearMocks(
                yearSelectionFortuneRepository,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getYearPillarPort,
                yearSelectionFortuneAiPort,
            )
        }

        beforeTest {
            every { yearSelectionFortuneRepository.lock(memberId, year) } just Runs
        }

        fun stubGeneration(generated: GeneratedYearSelectionFortune) {
            every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns null
            every { getMemberFortuneProfilePort.getProfile(memberId) } returns memberFortuneProfile()
            every { getSajuChartPort.getChart(memberId) } returns sajuChartSummary()
            every { getYearPillarPort.getPillar(year) } returns pillarSummary()
            every { yearSelectionFortuneAiPort.generate(any(), year, any()) } returns generated
        }

        describe("create") {
            context("이미 그 연도의 연도별 운세가 있으면") {
                it("AI를 재호출하지 않고, 저장된 연도별 운세를 반환한다") {
                    val existing = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    every { yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year) } returns existing

                    val result = service.create(year, memberId)

                    result.id shouldBe existing.id
                    verify(exactly = 0) { yearSelectionFortuneAiPort.generate(any(), any(), any()) }
                }
            }

            context("아직 생성된 적이 없으면") {
                it("회원 정보·사주·세운으로 AI를 호출해 연도별 운세를 저장하고 반환한다") {
                    val saved = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    stubGeneration(generatedYearSelectionFortune())
                    every { yearSelectionFortuneRepository.save(any()) } returns saved

                    val result = service.create(year, memberId)

                    result.id shouldBe saved.id
                    verify(exactly = 1) { yearSelectionFortuneRepository.save(any()) }
                }

                it("(memberId, year) 락을 선점한 뒤 조회한다") {
                    val saved = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    stubGeneration(generatedYearSelectionFortune())
                    every { yearSelectionFortuneRepository.save(any()) } returns saved

                    service.create(year, memberId)

                    verifyOrder {
                        yearSelectionFortuneRepository.lock(memberId, year)
                        yearSelectionFortuneRepository.findByMemberIdAndYear(memberId, year)
                    }
                }
            }

            context("AI가 반환한 카테고리가 중복되면") {
                it("YearSelectionFortuneCategoryDuplicatedException을 던진다") {
                    val duplicatedCategories =
                        listOf(
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 3),
                            GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
                        )
                    stubGeneration(generatedYearSelectionFortune(fortuneCategories = duplicatedCategories))

                    shouldThrow<YearSelectionFortuneCategoryDuplicatedException> { service.create(year, memberId) }

                    verify(exactly = 0) { yearSelectionFortuneRepository.save(any()) }
                }
            }
        }
    })

private fun pillarSummary(): PillarSummary =
    PillarSummary(
        stem = "갑",
        branch = "자",
        stemSipseong = "비견",
        branchSipseong = "정관",
        sibiunseong = "장생",
    )

private fun sajuChartSummary(): SajuChartSummary =
    SajuChartSummary(
        dayMaster = "갑",
        yearPillar = pillarSummary(),
        monthPillar = pillarSummary(),
        dayPillar = pillarSummary(),
        hourPillar = null,
        ohaeng = mapOf("목" to 3, "화" to 2),
        sipseong = mapOf("비견" to 2, "정관" to 1),
    )

private fun memberFortuneProfile(): MemberFortuneProfile =
    MemberFortuneProfile(
        name = "홍길동",
        birthDate = LocalDate.of(1998, 3, 5),
        gender = "MALE",
        job = "WORKER",
        relationshipStatus = "SOLO",
    )

private fun generatedYearSelectionFortune(
    fortuneCategories: List<GeneratedCategoryFortune> =
        listOf(
            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
            GeneratedCategoryFortune(FortuneCategory.MONEY, star = 3),
            GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
        ),
): GeneratedYearSelectionFortune =
    GeneratedYearSelectionFortune(
        title = "새해에는 좋은 기회가 많이 찾아와요",
        content = "전반적으로 안정적인 한 해가 될 것으로 보입니다.",
        score = 80,
        fortuneCategories = fortuneCategories,
    )
