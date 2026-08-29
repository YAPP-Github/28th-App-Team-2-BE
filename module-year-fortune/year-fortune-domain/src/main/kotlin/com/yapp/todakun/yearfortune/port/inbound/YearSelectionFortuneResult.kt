package com.yapp.todakun.yearfortune.port.inbound

import com.yapp.todakun.yearfortune.FortuneCategoryStar
import com.yapp.todakun.yearfortune.YearSelectionFortune
import java.util.UUID

/** 연도 선택 운세 생성·조회 결과. */
data class YearSelectionFortuneResult(
    val id: UUID,
    val year: Int,
    val score: Int,
    val title: String,
    val content: String,
    val fortuneCategories: List<FortuneCategoryStar>,
) {
    companion object {
        fun from(yearSelectionFortune: YearSelectionFortune): YearSelectionFortuneResult =
            YearSelectionFortuneResult(
                id = yearSelectionFortune.id,
                year = yearSelectionFortune.year,
                score = yearSelectionFortune.score,
                title = yearSelectionFortune.title,
                content = yearSelectionFortune.content,
                fortuneCategories = yearSelectionFortune.fortuneCategories,
            )
    }
}
