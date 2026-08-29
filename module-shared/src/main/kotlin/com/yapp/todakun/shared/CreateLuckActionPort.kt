package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

interface CreateLuckActionPort {
    fun create(
        memberId: UUID,
        fortuneCategory: FortuneCategory,
        fortuneDate: LocalDate,
        score: Int,
        title: String,
        content: String,
    ): UUID
}
