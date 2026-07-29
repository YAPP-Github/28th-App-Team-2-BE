package com.yapp.todakun.saju.application

import com.yapp.todakun.common.annotation.QueryService
import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import com.yapp.todakun.saju.PillarType
import com.yapp.todakun.saju.SajuCalculator
import com.yapp.todakun.saju.port.outbound.ManseryeokPort
import com.yapp.todakun.shared.GetDailyPillarPort
import com.yapp.todakun.shared.PillarSummary
import java.time.LocalDate

/**
 * 특정 날짜의 일진(당일 간지) 조회 크로스 도메인 유스케이스([GetDailyPillarPort] 구현).
 * 회원 정보 없이 달력 연산만으로 정해지는 값이라, [fortuneDate]를 출생일처럼 만세력 계산에 넣어 일주만 취한다.
 */
@QueryService
class GetDailyPillarService(
    private val manseryeokPort: ManseryeokPort,
) : GetDailyPillarPort {
    override fun getPillar(fortuneDate: LocalDate): PillarSummary {
        val fourPillars =
            manseryeokPort.calculate(
                birthDate = fortuneDate,
                birthTime = BirthTime.UNKNOWN,
                calendarType = CalendarType.SOLAR,
                isLeapMonth = false,
            )
        val dayPillar =
            SajuCalculator
                .pillars(fourPillars, fourPillars.day.stem)
                .first { it.pillarType == PillarType.DAY }

        return PillarSummary(
            stem = dayPillar.stem.reading,
            branch = dayPillar.branch.reading,
            stemSipseong = dayPillar.stemSipseong?.label,
            branchSipseong = dayPillar.branchSipseong.label,
            sibiunseong = dayPillar.sibiunseong.label,
        )
    }
}
