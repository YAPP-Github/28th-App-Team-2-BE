package com.yapp.todakun.dayfortune.port.inbound

import com.yapp.todakun.dayfortune.DaySelectionFortune
import com.yapp.todakun.dayfortune.DaySelectionPurpose
import com.yapp.todakun.dayfortune.FortuneCategoryStar
import java.time.LocalDate
import java.util.UUID

/** 택일 운세 생성·조회 결과. */
data class DaySelectionFortuneResult(
    val id: UUID,
    val purpose: DaySelectionPurpose,
    val targetDate: LocalDate,
    val score: Int,
    val title: String,
    val content: String,
    val fortuneCategories: List<FortuneCategoryStar>,
) {
    companion object {
        fun from(daySelectionFortune: DaySelectionFortune): DaySelectionFortuneResult =
            DaySelectionFortuneResult(
                id = daySelectionFortune.id,
                purpose = daySelectionFortune.purpose,
                targetDate = daySelectionFortune.targetDate,
                score = daySelectionFortune.score,
                title = daySelectionFortune.title,
                content = daySelectionFortune.content,
                fortuneCategories = daySelectionFortune.fortuneCategories,
            )
    }
}
