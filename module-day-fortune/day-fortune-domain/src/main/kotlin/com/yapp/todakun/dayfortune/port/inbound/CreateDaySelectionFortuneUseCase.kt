package com.yapp.todakun.dayfortune.port.inbound

import com.yapp.todakun.dayfortune.DaySelectionPurpose
import java.time.LocalDate
import java.util.UUID

interface CreateDaySelectionFortuneUseCase {
    fun create(
        purpose: DaySelectionPurpose,
        targetDates: List<LocalDate>,
        memberId: UUID,
    ): List<DaySelectionFortuneResult>
}
