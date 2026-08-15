package com.yapp.todakun.luck.adapter.web.dto.response

import com.yapp.todakun.luck.LuckAction
import com.yapp.todakun.shared.FortuneCategory
import java.util.UUID

data class LuckActionListResponse(
    val id: UUID,
    val fortuneCategory: FortuneCategory,
    val score: Int,
    val title: String,
    val achieved: Boolean,
) {
    companion object {
        fun from(luckAction: LuckAction) =
            LuckActionListResponse(
                id = luckAction.id,
                fortuneCategory = luckAction.fortuneCategory,
                score = luckAction.score,
                title = luckAction.title,
                achieved = luckAction.achieved,
            )
    }
}
