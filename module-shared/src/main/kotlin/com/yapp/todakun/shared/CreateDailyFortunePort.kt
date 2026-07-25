package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

interface CreateDailyFortunePort {
    fun create(
        memberId: UUID,
        fortuneDate: LocalDate,
        categoryScores: List<Int>,
        title: String,
        content: String,
        luckyItems: List<String>,
        cautionaryItems: List<String>,
    ): UUID
}
