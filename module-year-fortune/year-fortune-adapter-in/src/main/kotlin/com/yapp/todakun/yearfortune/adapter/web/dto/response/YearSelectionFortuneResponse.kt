package com.yapp.todakun.yearfortune.adapter.web.dto.response

import com.yapp.todakun.yearfortune.port.inbound.YearSelectionFortuneDetail
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
        fun from(detail: YearSelectionFortuneDetail) =
            YearSelectionFortuneResponse(
                id = detail.id,
                year = detail.year,
                score = detail.score,
                title = detail.title,
                content = detail.content,
                fortuneCategories = detail.fortuneCategories.map(FortuneCategoryStarResponse::from),
            )
    }
}
