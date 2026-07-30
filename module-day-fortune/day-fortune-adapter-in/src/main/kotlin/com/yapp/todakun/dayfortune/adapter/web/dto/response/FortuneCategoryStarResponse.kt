package com.yapp.todakun.dayfortune.adapter.web.dto.response

import com.yapp.todakun.dayfortune.FortuneCategoryStar
import com.yapp.todakun.shared.FortuneCategory

data class FortuneCategoryStarResponse(
    val fortuneCategory: FortuneCategory,
    val star: Int,
) {
    companion object {
        fun from(fortuneCategoryStar: FortuneCategoryStar) =
            FortuneCategoryStarResponse(
                fortuneCategory = fortuneCategoryStar.fortuneCategory,
                star = fortuneCategoryStar.star,
            )
    }
}
