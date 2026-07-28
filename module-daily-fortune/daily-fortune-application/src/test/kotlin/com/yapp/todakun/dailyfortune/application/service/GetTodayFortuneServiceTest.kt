package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetLuckActionScoresPort
import com.yapp.todakun.shared.LuckActionScore
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class GetTodayFortuneServiceTest :
    DescribeSpec(
        {
            val dailyFortuneRepository = mockk<DailyFortuneRepository>()
            val getLuckActionScoresPort = mockk<GetLuckActionScoresPort>()
            val getTodayFortuneService = GetTodayFortuneService(dailyFortuneRepository, getLuckActionScoresPort)

            afterTest { clearMocks(dailyFortuneRepository, getLuckActionScoresPort) }

            describe("getToday") {
                context("해당 날짜의 오늘의 운세가 있으면") {
                    it("행운 액션 점수와 함께 오늘의 운세 요약을 반환한다") {
                        val dailyFortune = DailyFortuneFixture.create()
                        val luckActionScores =
                            listOf(
                                LuckActionScore(id = Uuid.generateV7().toJavaUuid(), fortuneCategory = FortuneCategory.HEALTH, score = 80),
                            )
                        every {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)
                        } returns dailyFortune
                        every {
                            getLuckActionScoresPort.getScores(dailyFortune.memberId, dailyFortune.fortuneDate)
                        } returns luckActionScores

                        val summary = getTodayFortuneService.getToday(dailyFortune.memberId, dailyFortune.fortuneDate)

                        summary.id shouldBe dailyFortune.id
                        summary.fortuneDate shouldBe dailyFortune.fortuneDate
                        summary.score shouldBe dailyFortune.score
                        summary.title shouldBe dailyFortune.title
                        summary.luckActionScores shouldBe luckActionScores
                    }
                }

                context("서비스 데이·실제 캘린더 날짜 어느 쪽으로도 오늘의 운세가 존재하지 않으면") {
                    it("DailyFortuneNotFoundException을 던진다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val fortuneDate = LocalDate.of(2026, 6, 24)
                        every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate) } returns null
                        every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, any()) } returns null

                        shouldThrow<DailyFortuneNotFoundException> { getTodayFortuneService.getToday(memberId, fortuneDate) }
                    }
                }

                context("서비스 데이 기준으론 없지만 실제 캘린더 날짜로 생성된 오늘의 운세가 있으면") {
                    it("실제 캘린더 날짜 기준으로 조회해 반환하고, 행운 액션 점수도 같은 날짜로 조회한다") {
                        val serviceDate = LocalDate.of(2026, 6, 23)
                        val dailyFortune = DailyFortuneFixture.create(fortuneDate = LocalDate.of(2026, 6, 24))
                        val luckActionScores =
                            listOf(
                                LuckActionScore(id = Uuid.generateV7().toJavaUuid(), fortuneCategory = FortuneCategory.HEALTH, score = 80),
                            )
                        every {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, any())
                        } returns dailyFortune
                        every {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, serviceDate)
                        } returns null
                        every {
                            getLuckActionScoresPort.getScores(dailyFortune.memberId, dailyFortune.fortuneDate)
                        } returns luckActionScores

                        val summary = getTodayFortuneService.getToday(dailyFortune.memberId, serviceDate)

                        summary.id shouldBe dailyFortune.id
                        summary.fortuneDate shouldBe dailyFortune.fortuneDate
                        summary.luckActionScores shouldBe luckActionScores
                    }
                }
            }
        },
    )
