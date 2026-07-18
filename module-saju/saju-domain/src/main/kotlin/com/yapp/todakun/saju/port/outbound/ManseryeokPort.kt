package com.yapp.todakun.saju.port.outbound

import com.yapp.todakun.saju.BirthTime
import com.yapp.todakun.saju.CalendarType
import java.time.LocalDate

/**
 * 만세력(음양력 변환·60갑자·절기 기준 월주) 계산 아웃바운드 포트.
 * 현재 어댑터는 일주/년주/시주를 달력 연산으로 정확히 계산하고 월주는 절기 근사로 산출한다.
 * 추후 검증된 manseryeok 라이브러리 포팅본으로 어댑터만 교체하면 이 계약은 그대로 유지된다.
 */
interface ManseryeokPort {
    fun calculate(
        birthDate: LocalDate,
        birthTime: BirthTime,
        calendarType: CalendarType,
        isLeapMonth: Boolean,
    ): FourPillars
}
