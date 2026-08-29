package com.yapp.todakun.dailyfortune.port.inbound

import java.time.LocalDate
import java.util.UUID

/** [to]가 속한 달의 1일부터 [to]까지의 오늘의 운세 히스토리를 오래된 날짜순으로 반환한다. */
interface GetDailyFortuneHistoryUseCase {
    fun getHistory(
        memberId: UUID,
        today: LocalDate,
        to: LocalDate,
    ): List<DailyFortuneHistorySummary>
}
