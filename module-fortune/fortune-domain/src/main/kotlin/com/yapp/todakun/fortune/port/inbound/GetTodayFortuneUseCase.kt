package com.yapp.todakun.fortune.port.inbound

import java.time.LocalDate
import java.util.UUID

interface GetTodayFortuneUseCase {
    fun getToday(
        memberId: UUID,
        fortuneDate: LocalDate,
    ): TodayFortuneSummary
}
