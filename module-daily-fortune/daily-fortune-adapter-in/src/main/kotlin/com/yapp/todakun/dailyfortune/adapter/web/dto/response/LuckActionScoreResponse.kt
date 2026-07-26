package com.yapp.todakun.dailyfortune.adapter.web.dto.response

import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.shared.LuckActionScore
import java.util.UUID

data class LuckActionScoreResponse(
    val id: UUID,
    val fortuneCategory: FortuneCategory,
    val score: Int,
) {
    companion object {
        fun from(luckActionScore: LuckActionScore) =
            LuckActionScoreResponse(
                id = luckActionScore.id,
                fortuneCategory = luckActionScore.fortuneCategory,
                score = luckActionScore.score,
            )
    }
}
