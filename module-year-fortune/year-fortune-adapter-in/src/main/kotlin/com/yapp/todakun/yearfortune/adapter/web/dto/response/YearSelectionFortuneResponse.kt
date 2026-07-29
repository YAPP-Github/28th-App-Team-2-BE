package com.yapp.todakun.yearfortune.adapter.web.dto.response

import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneResult
import java.util.UUID

data class YearSelectionFortuneResponse(
    val id: UUID,
    val year: Int,
    val score: Int,
    val title: String,
    val content: String,
    val fortuneCategories: List<FortuneCategoryStarResponse>,
) {
    companion object {
        fun from(result: YearSelectionFortuneResult) =
            YearSelectionFortuneResponse(
                id = result.id,
                year = result.year,
                score = result.score,
                title = result.title,
                content = result.content,
                fortuneCategories = result.fortuneCategories.map(FortuneCategoryStarResponse::from),
            )
    }
}
