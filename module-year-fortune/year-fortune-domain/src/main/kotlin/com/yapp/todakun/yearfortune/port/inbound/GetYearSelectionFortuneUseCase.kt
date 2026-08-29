package com.yapp.todakun.yearfortune.port.inbound

import java.util.UUID

interface GetYearSelectionFortuneUseCase {
    fun getById(
        id: UUID,
        memberId: UUID,
    ): YearSelectionFortuneResult
}
