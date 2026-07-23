package com.yapp.todakun.fortune.port.inbound

import java.util.UUID

interface GetTodayFortuneUseCase {
    fun getToday(memberId: UUID): TodayFortuneSummary
}
