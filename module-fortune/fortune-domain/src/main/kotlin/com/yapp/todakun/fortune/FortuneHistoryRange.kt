package com.yapp.todakun.fortune

import com.yapp.todakun.fortune.exception.FortuneHistoryToOutOfRangeException
import java.time.LocalDate

/**
 * 오늘의 운세 히스토리 조회 범위: [to]가 속한 달의 1일부터 [to]까지.
 * [to]는 지난달 1일부터 [today]까지만 허용된다(지지난달 이전 조회 불가, 미래 날짜 조회 불가).
 */
fun fortuneHistoryRange(
    today: LocalDate,
    to: LocalDate,
): ClosedRange<LocalDate> {
    if (to !in today.minusMonths(1).withDayOfMonth(1)..today) { // 1일 ~ today
        throw FortuneHistoryToOutOfRangeException()
    }

    return to.withDayOfMonth(1)..to
}
