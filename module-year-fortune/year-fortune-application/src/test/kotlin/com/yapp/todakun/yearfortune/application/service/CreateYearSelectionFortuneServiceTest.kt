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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
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
        val yearSelectionFortuneTransactionalStore = mockk<YearSelectionFortuneTransactionalStore>()
        val getMemberFortuneProfilePort = mockk<GetMemberFortuneProfilePort>()
        val getSajuChartPort = mockk<GetSajuChartPort>()
        val getYearPillarPort = mockk<GetYearPillarPort>()
        val yearSelectionFortuneAiPort = mockk<YearSelectionFortuneAiPort>()
        val service =
            CreateYearSelectionFortuneService(
                yearSelectionFortuneTransactionalStore,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getYearPillarPort,
                yearSelectionFortuneAiPort,
            )

        val memberId = Uuid.generateV7().toJavaUuid()
        val year = 2026

        afterTest {
            clearMocks(
                yearSelectionFortuneTransactionalStore,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getYearPillarPort,
                yearSelectionFortuneAiPort,
            )
        }

        fun stubGeneration(generated: GeneratedYearSelectionFortune) {
            every { yearSelectionFortuneTransactionalStore.findExistingWithLock(memberId, year) } returns null
            every { getMemberFortuneProfilePort.getProfile(memberId) } returns memberFortuneProfile()
            every { getSajuChartPort.getChart(memberId) } returns sajuChartSummary()
            every { getYearPillarPort.getPillar(year) } returns pillarSummary()
            every { yearSelectionFortuneAiPort.generate(any(), year, any()) } returns generated
        }

        describe("create") {
            context("이미 그 연도의 연도별 운세가 있으면") {
                it("AI를 재호출하지 않고, 저장된 연도별 운세를 반환한다") {
                    val existing = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    every { yearSelectionFortuneTransactionalStore.findExistingWithLock(memberId, year) } returns existing

                    val result = service.create(year, memberId)

                    result.id shouldBe existing.id
                    verify(exactly = 0) { yearSelectionFortuneAiPort.generate(any(), any(), any()) }
                    verify(exactly = 0) { yearSelectionFortuneTransactionalStore.saveIfAbsent(any()) }
                }
            }

            context("아직 생성된 적이 없으면") {
                it("회원 정보·사주·세운으로 AI를 호출해 연도별 운세를 저장하고 반환한다") {
                    val saved = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    stubGeneration(generatedYearSelectionFortune())
                    every { yearSelectionFortuneTransactionalStore.saveIfAbsent(any()) } returns saved

                    val result = service.create(year, memberId)

                    result.id shouldBe saved.id
                    verify(exactly = 1) { yearSelectionFortuneTransactionalStore.saveIfAbsent(any()) }
                }

                it("선조회 트랜잭션 → (트랜잭션 밖) AI 호출 → 저장 트랜잭션 순서로 처리한다") {
                    val saved = YearSelectionFortuneFixture.create(memberId = memberId, year = year)
                    stubGeneration(generatedYearSelectionFortune())
                    every { yearSelectionFortuneTransactionalStore.saveIfAbsent(any()) } returns saved

                    service.create(year, memberId)

                    verifyOrder {
                        yearSelectionFortuneTransactionalStore.findExistingWithLock(memberId, year)
                        yearSelectionFortuneAiPort.generate(any(), year, any())
                        yearSelectionFortuneTransactionalStore.saveIfAbsent(any())
                    }
                }
            }

            context("AI가 반환한 카테고리가 중복되면") {
                it("YearSelectionFortuneCategoryDuplicatedException을 던지고 저장하지 않는다") {
                    val duplicatedCategories =
                        listOf(
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 3),
                            GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
                        )
                    stubGeneration(generatedYearSelectionFortune(fortuneCategories = duplicatedCategories))

                    shouldThrow<YearSelectionFortuneCategoryDuplicatedException> { service.create(year, memberId) }

                    verify(exactly = 0) { yearSelectionFortuneTransactionalStore.saveIfAbsent(any()) }
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
