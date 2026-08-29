package com.yapp.todakun.shared

import java.time.LocalDate
import java.util.UUID

/** 오늘의 운세 히스토리 화면에서 날짜별로 보여줄 행운 액션 한 건(제목·달성 여부 포함). */
data class LuckActionSummary(
    val id: UUID,
    val fortuneDate: LocalDate,
    val fortuneCategory: FortuneCategory,
    val title: String,
    val achieved: Boolean,
)
