package com.yapp.todakun.yearfortune.repository

import com.yapp.todakun.yearfortune.YearSelectionFortune
import java.util.UUID

interface YearSelectionFortuneRepository {
    fun findByMemberIdAndYear(
        memberId: UUID,
        year: Int,
    ): YearSelectionFortune?
}
