package com.yapp.todakun.saju.application

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.SajuCalculator
import com.yapp.todakun.saju.exception.SajuCalculationException
import com.yapp.todakun.saju.fixture.SajuFixture
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate

class GetDailyPillarServiceTest : DescribeSpec({
    val manseryeokPort = mockk<ManseryeokPort>()
    val service = GetDailyPillarService(manseryeokPort)

    afterTest { clearMocks(manseryeokPort) }

    describe("getPillar") {
        context("만세력 계산이 성공하면") {
            it("그 날짜를 출생일처럼 넘겨 계산한 일주를 PillarSummary로 반환한다") {
                val fortuneDate = LocalDate.of(2026, 7, 28)
                val fourPillars = SajuFixture.fourPillars()
                val expectedDayPillar = SajuCalculator.pillars(fourPillars, fourPillars.day.stem).first { it.pillarType == PillarType.DAY }
                every { manseryeokPort.calculate(fortuneDate, BirthTime.UNKNOWN, CalendarType.SOLAR, false) } returns fourPillars

                val result = service.getPillar(fortuneDate)

                result.stem shouldBe expectedDayPillar.stem.name
                result.branch shouldBe expectedDayPillar.branch.name
                result.stemSipseong shouldBe expectedDayPillar.stemSipseong?.name
                result.branchSipseong shouldBe expectedDayPillar.branchSipseong.name
                result.sibiunseong shouldBe expectedDayPillar.sibiunseong.name
                verify(exactly = 1) { manseryeokPort.calculate(fortuneDate, BirthTime.UNKNOWN, CalendarType.SOLAR, false) }
            }
        }

        context("만세력 계산이 실패하면") {
            it("SajuCalculationException을 그대로 전파한다") {
                val fortuneDate = LocalDate.of(2026, 7, 28)
                every {
                    manseryeokPort.calculate(fortuneDate, BirthTime.UNKNOWN, CalendarType.SOLAR, false)
                } throws SajuCalculationException()

                shouldThrow<SajuCalculationException> { service.getPillar(fortuneDate) }
            }
        }
    }
})
