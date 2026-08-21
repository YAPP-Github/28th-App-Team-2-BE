package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneNotFoundException
import com.yapp.todakun.dailyfortune.port.inbound.TodayFortuneSummary
import com.yapp.todakun.shared.CreateDailyFortunePort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
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
class GetTodayFortuneServiceTest :
    DescribeSpec(
        {
            val todayFortuneReader = mockk<TodayFortuneReader>()
            val createDailyFortunePort = mockk<CreateDailyFortunePort>()
            val getTodayFortuneService = GetTodayFortuneService(todayFortuneReader, createDailyFortunePort)

            afterTest { clearMocks(todayFortuneReader, createDailyFortunePort) }

            val memberId = Uuid.generateV7().toJavaUuid()
            val fortuneDate = LocalDate.of(2026, 6, 24)
            val summary =
                TodayFortuneSummary(
                    id = Uuid.generateV7().toJavaUuid(),
                    fortuneDate = fortuneDate,
                    score = 80,
                    title = "오늘은 활기찬 하루가 될 거예요",
                    luckActionScores = emptyList(),
                )

            describe("getToday") {
                context("이미 생성된 오늘의 운세가 있으면") {
                    it("생성을 호출하지 않고 그대로 반환한다") {
                        every { todayFortuneReader.find(memberId, fortuneDate) } returns summary

                        getTodayFortuneService.getToday(memberId, fortuneDate) shouldBe summary

                        verify(exactly = 0) { createDailyFortunePort.create(any(), any()) }
                    }
                }

                context("아직 생성되지 않았으면(가입 직후 AI 실패·배치 skip)") {
                    it("조회 시점에 생성한 뒤 다시 조회해 반환한다") {
                        every { todayFortuneReader.find(memberId, fortuneDate) } returnsMany listOf(null, summary)
                        every { createDailyFortunePort.create(memberId, any()) } returns summary.id

                        getTodayFortuneService.getToday(memberId, fortuneDate) shouldBe summary

                        verify(exactly = 1) { createDailyFortunePort.create(memberId, any()) }
                    }
                }

                context("생성까지 했는데도 조회되지 않으면") {
                    it("DailyFortuneNotFoundException을 던진다") {
                        every { todayFortuneReader.find(memberId, fortuneDate) } returns null
                        every { createDailyFortunePort.create(memberId, any()) } returns summary.id

                        shouldThrow<DailyFortuneNotFoundException> { getTodayFortuneService.getToday(memberId, fortuneDate) }
                    }
                }
            }
        },
    )
