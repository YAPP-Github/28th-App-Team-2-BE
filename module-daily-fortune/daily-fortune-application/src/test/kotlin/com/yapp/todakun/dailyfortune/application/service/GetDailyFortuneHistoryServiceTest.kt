package com.yapp.todakun.dailyfortune.application.service

import com.yapp.todakun.dailyfortune.exception.DailyFortuneHistoryToOutOfRangeException
import com.yapp.todakun.dailyfortune.fixture.DailyFortuneFixture
import com.yapp.todakun.dailyfortune.port.inbound.DailyFortuneHistorySummary
import com.yapp.todakun.dailyfortune.repository.DailyFortuneRepository
import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.GetLuckActionSummariesPort
import com.yapp.todakun.shared.LuckActionSummary
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

private val TODAY = LocalDate.of(2026, 7, 24)
private val RANGE_START = LocalDate.of(2026, 7, 1)
private val OUT_OF_RANGE_TO = LocalDate.of(2026, 5, 31)

@ExperimentalUuidApi
class GetDailyFortuneHistoryServiceTest :
    DescribeSpec(
        {
            val dailyFortuneRepository = mockk<DailyFortuneRepository>()
            val getLuckActionSummariesPort = mockk<GetLuckActionSummariesPort>()
            val getDailyFortuneHistoryService = GetDailyFortuneHistoryService(dailyFortuneRepository, getLuckActionSummariesPort)

            afterTest { clearMocks(dailyFortuneRepository, getLuckActionSummariesPort) }

            describe("getHistory") {
                context("to가 허용 범위 내이면") {
                    it("날짜별 행운 액션을 매핑한 오늘의 운세 히스토리를 반환한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val dailyFortune = DailyFortuneFixture.create(memberId = memberId, fortuneDate = LocalDate.of(2026, 7, 10))
                        val luckAction =
                            LuckActionSummary(
                                id = Uuid.generateV7().toJavaUuid(),
                                fortuneDate = dailyFortune.fortuneDate,
                                fortuneCategory = FortuneCategory.HEALTH,
                                title = "건강 챙기기",
                                achieved = true,
                            )
                        every {
                            dailyFortuneRepository.findAllByMemberIdBetweenFortuneDates(memberId, RANGE_START, TODAY)
                        } returns listOf(dailyFortune)
                        every {
                            getLuckActionSummariesPort.getSummaries(memberId, RANGE_START, TODAY)
                        } returns listOf(luckAction)

                        val history = getDailyFortuneHistoryService.getHistory(memberId, TODAY, TODAY)

                        history shouldBe listOf(DailyFortuneHistorySummary.from(dailyFortune, listOf(luckAction)))
                    }
                }

                context("해당 날짜에 매칭되는 행운 액션이 없으면") {
                    it("빈 리스트로 매핑한다") {
                        val memberId = Uuid.generateV7().toJavaUuid()
                        val dailyFortune = DailyFortuneFixture.create(memberId = memberId, fortuneDate = LocalDate.of(2026, 7, 10))
                        every {
                            dailyFortuneRepository.findAllByMemberIdBetweenFortuneDates(memberId, RANGE_START, TODAY)
                        } returns listOf(dailyFortune)
                        every {
                            getLuckActionSummariesPort.getSummaries(memberId, RANGE_START, TODAY)
                        } returns emptyList()

                        val history = getDailyFortuneHistoryService.getHistory(memberId, TODAY, TODAY)

                        history shouldBe listOf(DailyFortuneHistorySummary.from(dailyFortune, emptyList()))
                    }
                }

                context("to가 허용 범위를 벗어나면") {
                    it("DailyFortuneHistoryToOutOfRangeException을 던지고 조회하지 않는다") {
                        val memberId = Uuid.generateV7().toJavaUuid()

                        shouldThrow<DailyFortuneHistoryToOutOfRangeException> {
                            getDailyFortuneHistoryService.getHistory(memberId, TODAY, OUT_OF_RANGE_TO)
                        }

                        verify(exactly = 0) { dailyFortuneRepository.findAllByMemberIdBetweenFortuneDates(any(), any(), any()) }
                        verify(exactly = 0) { getLuckActionSummariesPort.getSummaries(any(), any(), any()) }
                    }
                }
            }
        },
    )
