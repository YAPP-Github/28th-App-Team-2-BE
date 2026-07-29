package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.SajuCalculator
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.shared.GetYearPillarPort
import com.yapp.todakun.shared.PillarSummary
import java.time.LocalDate

private const val REFERENCE_MONTH = 7
private const val REFERENCE_DAY = 1

/**
 * 특정 연도의 세운(연주) 조회 크로스 도메인 유스케이스([GetYearPillarPort] 구현).
 * 연주는 1/1이 아닌 입춘(2/4 전후) 기준으로 바뀌므로, 그 경계에 걸리지 않도록 해당 연도 7/1을 출생일처럼 넘겨 계산한 뒤 연주만 취한다.
 */
@QueryService
class GetYearPillarService(
    private val manseryeokPort: ManseryeokPort,
) : GetYearPillarPort {
    override fun getPillar(year: Int): PillarSummary {
        val fourPillars =
            manseryeokPort.calculate(
                birthDate = LocalDate.of(year, REFERENCE_MONTH, REFERENCE_DAY),
                birthTime = BirthTime.UNKNOWN,
                calendarType = CalendarType.SOLAR,
                isLeapMonth = false,
            )
        val yearPillar =
            SajuCalculator
                .pillars(fourPillars, fourPillars.day.stem)
                .first { it.pillarType == PillarType.YEAR }

        return PillarSummary(
            stem = yearPillar.stem.reading,
            branch = yearPillar.branch.reading,
            stemSipseong = yearPillar.stemSipseong?.label,
            branchSipseong = yearPillar.branchSipseong.label,
            sibiunseong = yearPillar.sibiunseong.label,
        )
    }
}
