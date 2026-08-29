package com.yapp.todakun.dayfortune.port.inbound

import java.util.UUID

interface GetDaySelectionFortuneUseCase {
    fun getById(
        id: UUID,
        memberId: UUID,
    ): DaySelectionFortuneResult
}
