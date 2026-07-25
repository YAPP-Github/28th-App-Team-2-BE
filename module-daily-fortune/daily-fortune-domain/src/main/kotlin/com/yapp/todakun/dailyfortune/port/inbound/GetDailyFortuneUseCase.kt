package com.yapp.todakun.dailyfortune.port.inbound

import java.util.UUID

interface GetDailyFortuneUseCase {
    fun getById(
        id: UUID,
        memberId: UUID,
    ): DailyFortuneDetail
}
