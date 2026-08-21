package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetLuckActionScoresPort
import com.yapp.todakun.shared.LuckActionScore
import com.yapp.todakun.shared.currentDate
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

@ExperimentalUuidApi
class TodayFortuneReaderTest :
    DescribeSpec(
        {
            val dailyFortuneRepository = mockk<DailyFortuneRepository>()
            val getLuckActionScoresPort = mockk<GetLuckActionScoresPort>()
            val todayFortuneReader = TodayFortuneReader(dailyFortuneRepository, getLuckActionScoresPort)

            afterTest { clearMocks(dailyFortuneRepository, getLuckActionScoresPort) }

            fun luckActionScores() =
                listOf(
                    LuckActionScore(id = Uuid.generateV7().toJavaUuid(), fortuneCategory = FortuneCategory.HEALTH, score = 80),
                )

            describe("find") {
                context("해당 날짜의 오늘의 운세가 있으면") {
                    it("행운 액션 점수와 함께 오늘의 운세 요약을 반환한다") {
                        val dailyFortune = DailyFortuneFixture.create()
                        val luckActionScores = luckActionScores()
                        every {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, dailyFortune.fortuneDate)
                        } returns dailyFortune
                        every {
                            getLuckActionScoresPort.getScores(dailyFortune.memberId, dailyFortune.fortuneDate)
                        } returns luckActionScores

                        val summary = todayFortuneReader.find(dailyFortune.memberId, dailyFortune.fortuneDate).shouldNotBeNull()

                        summary.id shouldBe dailyFortune.id
                        summary.fortuneDate shouldBe dailyFortune.fortuneDate
                        summary.score shouldBe dailyFortune.score
                        summary.title shouldBe dailyFortune.title
                        summary.luckActionScores shouldBe luckActionScores
                    }
                }

                context("서비스 데이·실제 캘린더 날짜 어느 쪽으로도 오늘의 운세가 존재하지 않으면") {
                    it("null을 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val fortuneDate = LocalDate.of(2026, 6, 24)
                        every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, fortuneDate) } returns null
                        every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, any()) } returns null

                        todayFortuneReader.find(memberId, fortuneDate).shouldBeNull()
                    }
                }

                context("서비스 데이 기준으론 없지만 실제 캘린더 날짜로 생성된 오늘의 운세가 있으면") {
                    it("실제 캘린더 날짜(currentDate) 기준으로 조회해 반환하고, 행운 액션 점수도 같은 날짜로 조회한다") {
                        val serviceDate = LocalDate.of(2026, 6, 23)
                        val calendarDate = currentDate()
                        val dailyFortune = DailyFortuneFixture.create(fortuneDate = calendarDate)
                        val luckActionScores = luckActionScores()
                        every {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, calendarDate)
                        } returns dailyFortune
                        every {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, serviceDate)
                        } returns null
                        every {
                            getLuckActionScoresPort.getScores(dailyFortune.memberId, calendarDate)
                        } returns luckActionScores

                        val summary = todayFortuneReader.find(dailyFortune.memberId, serviceDate).shouldNotBeNull()

                        summary.id shouldBe dailyFortune.id
                        summary.fortuneDate shouldBe calendarDate
                        summary.luckActionScores shouldBe luckActionScores
                        verify(exactly = 1) {
                            dailyFortuneRepository.findByMemberIdAndFortuneDate(dailyFortune.memberId, calendarDate)
                        }
                    }
                }

                context("서비스 데이가 실제 캘린더 날짜와 같으면(06:00 이후)") {
                    it("같은 조건으로 두 번 조회하지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val today = currentDate()
                        every { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, today) } returns null

                        todayFortuneReader.find(memberId, today).shouldBeNull()

                        verify(exactly = 1) { dailyFortuneRepository.findByMemberIdAndFortuneDate(memberId, today) }
                    }
                }
            }
        },
    )
