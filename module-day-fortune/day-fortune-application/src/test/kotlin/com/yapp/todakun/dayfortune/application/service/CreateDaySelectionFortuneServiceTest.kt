package com.yapp.todakun.dayfortune.application.service

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.exception.DaySelectionFortuneCategoryDuplicatedException
import com.yapp.todakun.dayfortune.fixture.DaySelectionFortuneFixture
import com.yapp.todakun.dayfortune.port.outbound.DaySelectionFortuneAiPort
import com.yapp.todakun.dayfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dayfortune.port.outbound.GeneratedDaySelectionFortune
import com.yapp.todakun.dayfortune.repository.DaySelectionFortuneRepository
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
class CreateDaySelectionFortuneServiceTest :
    DescribeSpec({
        val daySelectionFortuneRepository = mockk<DaySelectionFortuneRepository>()
        val getMemberFortuneProfilePort = mockk<GetMemberFortuneProfilePort>()
        val getSajuChartPort = mockk<GetSajuChartPort>()
        val getDailyPillarPort = mockk<GetDailyPillarPort>()
        val daySelectionFortuneAiPort = mockk<DaySelectionFortuneAiPort>()
        val service =
            CreateDaySelectionFortuneService(
                daySelectionFortuneRepository,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getDailyPillarPort,
                daySelectionFortuneAiPort,
            )

        val memberId = Uuid.generateV7().toJavaUuid()
        val purpose = DaySelectionPurpose.TRAVEL
        val targetDate = LocalDate.of(2026, 8, 1)

        afterTest {
            clearMocks(
                daySelectionFortuneRepository,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getDailyPillarPort,
                daySelectionFortuneAiPort,
            )
        }

        beforeTest {
            every { daySelectionFortuneRepository.lock(memberId, purpose, any()) } just Runs
            every { getMemberFortuneProfilePort.getProfile(memberId) } returns memberFortuneProfile()
            every { getSajuChartPort.getChart(memberId) } returns sajuChartSummary()
        }

        fun stubGeneration(
            date: LocalDate,
            generated: GeneratedDaySelectionFortune,
        ) {
            every { daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, date) } returns null
            every { getDailyPillarPort.getPillar(date) } returns pillarSummary()
            every { daySelectionFortuneAiPort.generate(any(), purpose, date, any()) } returns generated
        }

        describe("create") {
            context("이미 그 (목적, 날짜) 조합의 택일 운세가 있으면") {
                it("AI를 재호출하지 않고, 저장된 택일 운세를 반환한다") {
                    val existing = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate)
                    every {
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                    } returns existing

                    val result = service.create(purpose, listOf(targetDate), memberId)

                    result.map { it.id } shouldBe listOf(existing.id)
                    verify(exactly = 0) { daySelectionFortuneAiPort.generate(any(), any(), any(), any()) }
                }
            }

            context("아직 생성된 적이 없으면") {
                it("회원 정보·사주·일진으로 AI를 호출해 택일 운세를 저장하고 반환한다") {
                    val saved = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate)
                    stubGeneration(targetDate, generatedDaySelectionFortune())
                    every { daySelectionFortuneRepository.save(any()) } returns saved

                    val result = service.create(purpose, listOf(targetDate), memberId)

                    result.map { it.id } shouldBe listOf(saved.id)
                    verify(exactly = 1) { daySelectionFortuneRepository.save(any()) }
                }

                it("(memberId, purpose, targetDate) 락을 선점한 뒤 조회한다") {
                    val saved = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = targetDate)
                    stubGeneration(targetDate, generatedDaySelectionFortune())
                    every { daySelectionFortuneRepository.save(any()) } returns saved

                    service.create(purpose, listOf(targetDate), memberId)

                    verifyOrder {
                        daySelectionFortuneRepository.lock(memberId, purpose, targetDate)
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, targetDate)
                    }
                }
            }

            context("후보 날짜 중 일부만 이미 생성되어 있으면") {
                it("이미 생성된 날짜는 재사용하고, 나머지 날짜만 AI를 호출해 생성한다") {
                    val existingDate = LocalDate.of(2026, 8, 1)
                    val newDate = LocalDate.of(2026, 8, 15)
                    val existing = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = existingDate)
                    val saved = DaySelectionFortuneFixture.create(memberId = memberId, purpose = purpose, targetDate = newDate)
                    every {
                        daySelectionFortuneRepository.findByMemberIdAndPurposeAndTargetDate(memberId, purpose, existingDate)
                    } returns existing
                    stubGeneration(newDate, generatedDaySelectionFortune())
                    every { daySelectionFortuneRepository.save(any()) } returns saved

                    val result = service.create(purpose, listOf(existingDate, newDate), memberId)

                    result.map { it.id } shouldBe listOf(existing.id, saved.id)
                    verify(exactly = 1) { daySelectionFortuneAiPort.generate(any(), purpose, newDate, any()) }
                    verify(exactly = 0) { daySelectionFortuneAiPort.generate(any(), purpose, existingDate, any()) }
                }
            }

            context("AI가 반환한 카테고리가 중복되면") {
                it("DaySelectionFortuneCategoryDuplicatedException을 던진다") {
                    val duplicatedCategories =
                        listOf(
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 2),
                            GeneratedCategoryFortune(FortuneCategory.RELATIONSHIP, star = 3),
                            GeneratedCategoryFortune(FortuneCategory.LOVE, star = 1),
                        )
                    stubGeneration(targetDate, generatedDaySelectionFortune(fortuneCategories = duplicatedCategories))

                    shouldThrow<DaySelectionFortuneCategoryDuplicatedException> {
                        service.create(purpose, listOf(targetDate), memberId)
                    }

                    verify(exactly = 0) { daySelectionFortuneRepository.save(any()) }
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
        favoriteFortuneCategories = listOf(FortuneCategory.LOVE, FortuneCategory.MONEY),
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
