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

                context("해당 날짜의 오늘의 운세가 존재하지 않으면") {
                    it("DailyFortuneNotFoundException을 던진다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val fortuneDate = LocalDate.of(2026, 6, 24)
                        every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate) } returns null

                        shouldThrow<DailyFortuneNotFoundException> { getTodayFortuneService.getToday(memberId, fortuneDate) }
                    }
                }
            }
        },
    )
