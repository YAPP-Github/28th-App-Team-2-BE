package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneGenerationFailedException
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.port.outbound.DailyFortuneAiPort
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedCategoryFortune
import com.yapp.todakun.dailyfortune.port.outbound.GeneratedDailyFortune
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
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

private val MEMBER_ID = UUID.fromString("018f0000-0000-7000-8000-000000000002")

@ExperimentalUuidApi
class CreateDailyFortuneServiceTest :
    DescribeSpec({
        val dailyFortuneTransactionalStore = mockk<DailyFortuneTransactionalStore>()
        val getMemberFortuneProfilePort = mockk<GetMemberFortuneProfilePort>()
        val getSajuChartPort = mockk<GetSajuChartPort>()
        val getDailyPillarPort = mockk<GetDailyPillarPort>()
        val dailyFortuneAiPort = mockk<DailyFortuneAiPort>()
        val service =
            CreateDailyFortuneService(
                dailyFortuneTransactionalStore,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getDailyPillarPort,
                dailyFortuneAiPort,
            )

        val fortuneDate = LocalDate.of(2026, 6, 24)

        afterTest {
            clearMocks(
                dailyFortuneTransactionalStore,
                getMemberFortuneProfilePort,
                getSajuChartPort,
                getDailyPillarPort,
                dailyFortuneAiPort,
            )
        }

        fun stubGeneration(generated: GeneratedDailyFortune) {
            every { dailyFortuneTransactionalStore.findExistingWithLock(MEMBER_ID, fortuneDate) } returns null
            every { getMemberFortuneProfilePort.getProfile(MEMBER_ID) } returns memberFortuneProfile()
            every { getSajuChartPort.getChart(MEMBER_ID) } returns sajuChartSummary()
            every { getDailyPillarPort.getPillar(fortuneDate) } returns pillarSummary()
            every { dailyFortuneAiPort.generate(any(), fortuneDate, any()) } returns generated
        }

        describe("create") {
            context("이미 그 날짜의 오늘의 운세가 있으면") {
                it("AI를 재호출하지 않고, 저장된 오늘의 운세의 ID를 반환한다") {
                    val existing = DailyFortuneFixture.create(memberId = MEMBER_ID, fortuneDate = fortuneDate)
                    every { dailyFortuneTransactionalStore.findExistingWithLock(MEMBER_ID, fortuneDate) } returns existing

                    val result = service.create(MEMBER_ID, fortuneDate)

                    result shouldBe existing.id
                    verify(exactly = 0) { dailyFortuneAiPort.generate(any(), any(), any()) }
                    verify(exactly = 0) { dailyFortuneTransactionalStore.saveIfAbsent(any(), any()) }
                }
            }

            context("아직 생성된 적이 없으면") {
                it("회원 정보·사주로 AI를 호출해 오늘의 운세와 LuckAction을 멱등 저장하고 ID를 반환한다") {
                    val saved = DailyFortuneFixture.create(memberId = MEMBER_ID, fortuneDate = fortuneDate)
                    stubGeneration(generatedDailyFortune())
                    every { dailyFortuneTransactionalStore.saveIfAbsent(any(), any()) } returns saved.id

                    val result = service.create(MEMBER_ID, fortuneDate)

                    result shouldBe saved.id
                    verify(exactly = 1) { dailyFortuneTransactionalStore.saveIfAbsent(any(), any()) }
                }

                it("선조회 트랜잭션 → (트랜잭션 밖) AI 호출 → 저장 트랜잭션 순서로 처리한다") {
                    val saved = DailyFortuneFixture.create(memberId = MEMBER_ID, fortuneDate = fortuneDate)
                    stubGeneration(generatedDailyFortune())
                    every { dailyFortuneTransactionalStore.saveIfAbsent(any(), any()) } returns saved.id

                    service.create(MEMBER_ID, fortuneDate)

                    verifyOrder {
                        dailyFortuneTransactionalStore.findExistingWithLock(MEMBER_ID, fortuneDate)
                        dailyFortuneAiPort.generate(any(), fortuneDate, any())
                        dailyFortuneTransactionalStore.saveIfAbsent(any(), any())
                    }
                }
            }

            context("AI가 카테고리를 정확히 5개(1개씩) 채우지 못하면") {
                it("DailyFortuneGenerationFailedException을 던지고 저장하지 않는다") {
                    val duplicatedCategories =
                        listOf(
                            FortuneCategory.RELATIONSHIP,
                            FortuneCategory.LOVE,
                            FortuneCategory.LOVE,
                            FortuneCategory.ACHIEVEMENT,
                            FortuneCategory.HEALTH,
                        )
                    stubGeneration(generatedDailyFortune(categories = duplicatedCategories))

                    shouldThrow<DailyFortuneGenerationFailedException> { service.create(MEMBER_ID, fortuneDate) }

                    verify(exactly = 0) { dailyFortuneTransactionalStore.saveIfAbsent(any(), any()) }
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

private fun generatedDailyFortune(categories: List<FortuneCategory> = FortuneCategory.entries): GeneratedDailyFortune =
    GeneratedDailyFortune(
        title = "오늘은 새로운 기회가 찾아옵니다",
        content = "오늘의 운세 종합 해석 내용입니다.",
        luckyItems = listOf("파란색", "지갑", "커피", "책", "우산"),
        cautionaryItems = listOf("빨간색", "가위", "동전", "성냥", "칼"),
        categoryFortunes =
            categories.map {
                GeneratedCategoryFortune(
                    fortuneCategory = it,
                    score = 70,
                    title = "오늘의 액션",
                    content = "상세 해석",
                )
            },
    )
