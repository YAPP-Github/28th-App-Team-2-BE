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

class GetYearPillarServiceTest : DescribeSpec({
    val manseryeokPort = mockk<ManseryeokPort>()
    val service = GetYearPillarService(manseryeokPort)

    val year = 2026
    val referenceDate = LocalDate.of(year, 7, 1)

    afterTest { clearMocks(manseryeokPort) }

    describe("getPillar") {
        context("만세력 계산이 성공하면") {
            it("그 연도의 7/1을 출생일처럼 넘겨 계산한 연주를 PillarSummary로 반환한다") {
                val fourPillars = SajuFixture.fourPillars()
                val expectedYearPillar =
                    SajuCalculator.pillars(
                        fourPillars,
                        fourPillars.day.stem,
                    ).first { it.pillarType == PillarType.YEAR }
                every { manseryeokPort.calculate(referenceDate, BirthTime.UNKNOWN, CalendarType.SOLAR, false) } returns fourPillars

                val result = service.getPillar(year)

                result.stem shouldBe expectedYearPillar.stem.reading
                result.branch shouldBe expectedYearPillar.branch.reading
                result.stemSipseong shouldBe expectedYearPillar.stemSipseong?.label
                result.branchSipseong shouldBe expectedYearPillar.branchSipseong.label
                result.sibiunseong shouldBe expectedYearPillar.sibiunseong.label
                verify(exactly = 1) { manseryeokPort.calculate(referenceDate, BirthTime.UNKNOWN, CalendarType.SOLAR, false) }
            }
        }

        context("만세력 계산이 실패하면") {
            it("SajuCalculationException을 던진다") {
                every {
                    manseryeokPort.calculate(referenceDate, BirthTime.UNKNOWN, CalendarType.SOLAR, false)
                } throws SajuCalculationException()

                shouldThrow<SajuCalculationException> { service.getPillar(year) }
            }
        }
    }
})
