package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneCategoryDuplicatedException
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dayfortune.port.outbound.GeneratedDaySelectionFortune
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.GetMemberFortuneProfilePort
import com.yapp.todakun.shared.GetSajuChartPort
import com.yapp.todakun.shared.MemberFortuneProfile
import com.yapp.todakun.shared.PillarSummary
import com.yapp.todakun.shared.SajuChartSummary
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
class CreateOneDaySelectionFortuneServiceTest :
    DescribeSpec({
        val daySelectionFortuneTransactionalStore = mockk<DaySelectionFortuneTransactionalStore>()
        val getMemberFortuneProfilePort = mockk<GetMemberFortuneProfilePort>()
        val getSajuChartPort = mockk<GetSajuChartPort>()
        val getDailyPillarPort = mockk<GetDailyPillarPort>()
        val daySelectionFortuneAiPort = mockk<DaySelectionFortuneAiPort>()
        val service =
            CreateOneDaySelectionFortuneService(
                daySelectionFortuneTransactionalStore,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getDailyPillarPort,
                daySelectionFortuneAiPort,
            )

        val memberId = Uuid.generateV7().toJavaUuid()
        val purpose = DaySelectionPurpose.TRAVEL
        val targetDate = LocalDate.now().plusDays(30)

        afterTest {
            clearMocks(
                daySelectionFortuneTransactionalStore,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getDailyPillarPort,
                daySelectionFortuneAiPort,
            )
        }

        fun stubGeneration(generated: GeneratedDaySelectionFortune) {
            every { daySelectionFortuneTransactionalStore.findExistingWithLock(memberId, purpose, targetDate) } returns null
            every { getMemberFortuneProfilePort.getProfile(memberId) } returns memberFortuneProfile()
            every { getSajuChartPort.getChart(memberId) } returns sajuChartSummary()
            every { getDailyPillarPort.getPillar(targetDate) } returns pillarSummary()
            every { daySelectionFortuneAiPort.generate(any(), purpose, targetDate, any()) } returns generated
        }

        describe("createOne") {
            context("이미 (목적, 날짜) 조합의 택일 운세가 있으면") {
                it("회원 프로필/사주 조회와 AI 호출 없이 저장된 택일 운세를 반환한다") {
                    val existing = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate)
                    every {
                        daySelectionFortuneTransactionalStore.findExistingWithLock(memberId, purpose, targetDate)
                    } returns existing

                    val result = service.createOne(purpose, targetDate, memberId)

                    result.id shouldBe existing.id
                    verify(exactly = 0) { getMemberFortuneProfilePort.getProfile(any()) }
                    verify(exactly = 0) { getSajuChartPort.getChart(any()) }
                    verify(exactly = 0) { getDailyPillarPort.getPillar(any()) }
                    verify(exactly = 0) { daySelectionFortuneAiPort.generate(any(), any(), any(), any()) }
                    verify(exactly = 0) { daySelectionFortuneTransactionalStore.saveIfAbsent(any()) }
                }
            }

            context("아직 생성된 적이 없으면") {
                it("회원 정보/사주/일진으로 AI를 호출해 택일 운세를 저장하고 반환한다") {
                    val saved = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate)
                    stubGeneration(generatedDaySelectionFortune())
                    every { daySelectionFortuneTransactionalStore.saveIfAbsent(any()) } returns saved

                    val result = service.createOne(purpose, targetDate, memberId)

                    result.id shouldBe saved.id
                    verify(exactly = 1) { daySelectionFortuneTransactionalStore.saveIfAbsent(any()) }
                }

                it("선조회 트랜잭션 → (트랜잭션 밖) AI 호출 → 저장 트랜잭션 순서로 처리한다") {
                    val saved = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate)
                    stubGeneration(generatedDaySelectionFortune())
                    every { daySelectionFortuneTransactionalStore.saveIfAbsent(any()) } returns saved

                    service.createOne(purpose, targetDate, memberId)

                    verifyOrder {
                        daySelectionFortuneTransactionalStore.findExistingWithLock(memberId, purpose, targetDate)
                        daySelectionFortuneAiPort.generate(any(), purpose, targetDate, any())
                        daySelectionFortuneTransactionalStore.saveIfAbsent(any())
                    }
                }
            }

            context("AI가 반환한 카테고리가 중복되면") {
                it("DaySelectionFortuneCategoryDuplicatedException을 던지고 저장하지 않는다") {
                    val duplicatedCategories =
                        listOf(
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 3),
                            GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
                        )
                    stubGeneration(generatedDaySelectionFortune(fortuneCategories = duplicatedCategories))

                    shouldThrow<DaySelectionFortuneCategoryDuplicatedException> {
                        service.createOne(purpose, targetDate, memberId)
                    }

                    verify(exactly = 0) { daySelectionFortuneTransactionalStore.saveIfAbsent(any()) }
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

private fun generatedDaySelectionFortune(
    fortuneCategories: List<GeneratedCategoryFortune> =
        listOf(
            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
            GeneratedCategoryFortune(FortuneCategory.MONEY, star = 3),
            GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
        ),
): GeneratedDaySelectionFortune =
    GeneratedDaySelectionFortune(
        title = "이 날은 여행하기 좋은 기운이 감돌아요",
        content = "전반적으로 안정적인 기운이 흐르는 날입니다.",
        score = 80,
        fortuneCategories = fortuneCategories,
    )
