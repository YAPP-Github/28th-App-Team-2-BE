package com.yapp.todakun.yearfortune.port.inbound

import java.util.UUID

interface CreateYearSelectionFortuneUseCase {
    fun create(
        year: Int,
        memberId: UUID,
    ): YearSelectionFortuneResult
}
