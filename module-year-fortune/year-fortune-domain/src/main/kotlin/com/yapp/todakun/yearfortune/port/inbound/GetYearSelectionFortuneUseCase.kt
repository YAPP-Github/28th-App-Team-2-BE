package com.yapp.todakun.yearfortune.port.inbound

import java.util.UUID

interface GetYearSelectionFortuneUseCase {
    fun getByYear(
        year: Int,
        memberId: UUID,
    ): YearSelectionFortuneDetail
}
