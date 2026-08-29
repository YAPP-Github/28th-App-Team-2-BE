package com.yapp.todakun.dailyfortune.adapter.web.dto.response

import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.LuckActionSummary
import java.util.UUID

data class LuckActionSummaryResponse(
    val id: UUID,
    val fortuneCategory: FortuneCategory,
    val title: String,
    val achieved: Boolean,
) {
    companion object {
        fun from(luckActionSummary: LuckActionSummary) =
            LuckActionSummaryResponse(
                id = luckActionSummary.id,
                fortuneCategory = luckActionSummary.fortuneCategory,
                title = luckActionSummary.title,
                achieved = luckActionSummary.achieved,
            )
    }
}
