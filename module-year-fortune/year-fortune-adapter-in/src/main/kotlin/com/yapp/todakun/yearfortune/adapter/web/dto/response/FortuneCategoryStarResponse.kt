package com.yapp.todakun.yearfortune.adapter.web.dto.response

import com.yapp.todakun.shared.FortuneCategory
import com.yapp.todakun.yearfortune.FortuneCategoryStar

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
